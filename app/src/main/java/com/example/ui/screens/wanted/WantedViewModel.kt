package com.example.ui.screens.wanted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.AppSettings
import com.example.data.model.CachedFeedResult
import com.example.data.model.Listing
import com.example.data.model.ProvinceLocations
import com.example.data.model.SearchFilter
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WantedUiState(
    val query: String = "",
    val selectedProvince: String? = null,
    val sort: String = "newest",
    val provinces: List<ProvinceLocations> = MarketZedCatalogue.provinces,
    val listings: List<Listing> = emptyList(),
    val nextCursor: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isOffline: Boolean = false,
    val isStale: Boolean = false,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings()
)

class WantedViewModel(
    private val marketplaceRepository: MarketplaceRepository,
    private val localUserRepository: LocalUserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _province = MutableStateFlow<String?>(null)
    private val _sort = MutableStateFlow("newest")
    private val _liveResults = MutableStateFlow<List<Listing>?>(null)
    private val _nextCursor = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private var activeJob: Job? = null
    private var debounceJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoriteIdsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeFavoriteIds(auth.userScope)
    }

    val uiState: StateFlow<WantedUiState> = combine(
        marketplaceRepository.observeSectionFeed(MarketplaceRepository.SECTION_HOME_WANTED),
        marketplaceRepository.observeLocations(),
        favoriteIdsFlow,
        localUserRepository.observeSettings(),
        _query,
        _province,
        _sort,
        _liveResults,
        _nextCursor,
        _isLoading,
        _isRefreshing,
        _isLoadingMore,
        _errorMessage
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val cachedFeed = arr[0] as CachedFeedResult<Listing>
        @Suppress("UNCHECKED_CAST")
        val provinces = arr[1] as List<ProvinceLocations>
        @Suppress("UNCHECKED_CAST")
        val favIds = arr[2] as Set<String>
        val settings = arr[3] as AppSettings
        val query = arr[4] as String
        val province = arr[5] as String?
        val sort = arr[6] as String
        @Suppress("UNCHECKED_CAST")
        val liveResults = arr[7] as List<Listing>?
        val nextCursor = arr[8] as String?
        val isLoading = arr[9] as Boolean
        val isRefreshing = arr[10] as Boolean
        val isLoadingMore = arr[11] as Boolean
        val errorMessage = arr[12] as String?

        val filteredCached = filterWantedItems(cachedFeed.items, query, province, sort)
        val shown = liveResults ?: filteredCached

        WantedUiState(
            query = query,
            selectedProvince = province,
            sort = sort,
            provinces = provinces,
            listings = shown,
            nextCursor = nextCursor ?: if (liveResults == null) cachedFeed.nextCursor else null,
            isLoading = isLoading && shown.isEmpty(),
            isRefreshing = isRefreshing,
            isLoadingMore = isLoadingMore,
            isOffline = cachedFeed.isOffline,
            isStale = cachedFeed.isStale,
            lastSyncedAt = cachedFeed.lastSyncedAt,
            errorMessage = errorMessage ?: if (shown.isEmpty()) cachedFeed.errorMessage else null,
            favoriteIds = favIds,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WantedUiState()
    )

    init {
        refresh(force = false)
    }

    fun onQueryChanged(value: String) {
        _query.value = value
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(360)
            loadWanted(forceRefresh = true)
        }
    }

    fun selectProvince(province: String?) {
        _province.value = province
        loadWanted(forceRefresh = true)
    }

    fun selectSort(sort: String) {
        _sort.value = sort
        loadWanted(forceRefresh = true)
    }

    fun clearFilters() {
        _query.value = ""
        _province.value = null
        _sort.value = "newest"
        _liveResults.value = null
        _errorMessage.value = null
        loadWanted(forceRefresh = true)
    }

    fun refresh(force: Boolean = true) {
        loadWanted(forceRefresh = force)
    }

    fun loadNextPage() {
        val cursor = _nextCursor.value ?: return
        if (_isLoadingMore.value || _isRefreshing.value) return
        val filter = currentFilter()
        val requestKey = filter.cacheKey()
        _isLoadingMore.value = true
        viewModelScope.launch {
            marketplaceRepository.searchListings(filter, cursor = cursor, limit = 20)
                .onSuccess { page ->
                    if (currentFilter().cacheKey() == requestKey) {
                        val current = _liveResults.value ?: uiState.value.listings
                        _liveResults.value = (current + page.items).distinctBy { it.id }
                        _nextCursor.value = page.nextCursor
                    }
                }
                .onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Could not load more wanted requests."
                }
            _isLoadingMore.value = false
        }
    }

    fun toggleFavorite(listingId: String) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.toggleFavorite(scope, listingId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun currentFilter() = SearchFilter(
        query = _query.value.trim(),
        category = "wanted",
        province = _province.value,
        sort = _sort.value
    )

    private fun loadWanted(forceRefresh: Boolean) {
        activeJob?.cancel()
        val filter = currentFilter()
        val requestKey = filter.cacheKey()
        _errorMessage.value = null

        activeJob = viewModelScope.launch {
            val cachedMatches = marketplaceRepository.searchCachedOfflineSnapshot(filter)
            if (currentFilter().cacheKey() != requestKey) return@launch

            if (cachedMatches.isNotEmpty()) {
                _liveResults.value = cachedMatches
                _isLoading.value = false
                if (forceRefresh) _isRefreshing.value = true
            } else {
                _isLoading.value = true
                _isRefreshing.value = forceRefresh
            }

            if (filter.query.isBlank() && filter.province == null && filter.sort == "newest") {
                marketplaceRepository.refreshHomeFeeds(force = forceRefresh)
            }

            marketplaceRepository.searchListings(filter, cursor = null, limit = 20)
                .onSuccess { page ->
                    if (currentFilter().cacheKey() == requestKey) {
                        _liveResults.value = page.items.distinctBy { it.id }
                        _nextCursor.value = page.nextCursor
                        _errorMessage.value = null
                    }
                }
                .onFailure { err ->
                    if (currentFilter().cacheKey() == requestKey && cachedMatches.isEmpty()) {
                        _errorMessage.value = err.localizedMessage ?: "Could not load Wanted requests."
                    }
                }

            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    private fun filterWantedItems(
        items: List<Listing>,
        query: String,
        province: String?,
        sort: String
    ): List<Listing> {
        val q = query.trim().lowercase()
        return items
            .asSequence()
            .filter {
                (province.isNullOrBlank() || it.location?.province.equals(province, ignoreCase = true)) &&
                    (q.isBlank() || it.title.lowercase().contains(q) || it.description.lowercase().contains(q) || it.location?.town?.lowercase()?.contains(q) == true)
            }
            .sortedWith(
                if (sort == "oldest") compareBy { it.createdAtMillis }
                else compareByDescending { it.createdAtMillis }
            )
            .toList()
    }
}
