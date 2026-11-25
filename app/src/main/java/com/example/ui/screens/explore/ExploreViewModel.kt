package com.example.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.BudgetParseResult
import com.example.core.util.PriceFormatter
import com.example.data.auth.AuthRepository
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.AppSettings
import com.example.data.model.CategoryOption
import com.example.data.model.Listing
import com.example.data.model.ProvinceLocations
import com.example.data.model.SavedSearch
import com.example.data.model.SearchFilter
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.SponsoredRotationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val filter: SearchFilter = SearchFilter(),
    val minPriceText: String = "",
    val maxPriceText: String = "",
    val budgetError: String? = null,
    val categories: List<CategoryOption> = MarketZedCatalogue.categories,
    val provinces: List<ProvinceLocations> = MarketZedCatalogue.provinces,
    val results: List<Listing> = emptyList(),
    val sponsoredHighlights: List<Listing> = emptyList(),
    val nextCursor: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isOfflineFallback: Boolean = false,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null,
    val searchHistory: List<String> = emptyList(),
    val savedSearches: List<SavedSearch> = emptyList(),
    val saveSearchFeedback: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val hasSearched: Boolean = false
)

class ExploreViewModel(
    private val marketplaceRepository: MarketplaceRepository,
    private val localUserRepository: LocalUserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(SearchFilter())
    private val _minPriceText = MutableStateFlow("")
    private val _maxPriceText = MutableStateFlow("")
    private val _budgetError = MutableStateFlow<String?>(null)
    private val _results = MutableStateFlow<List<Listing>>(emptyList())
    private val _nextCursor = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _isOfflineFallback = MutableStateFlow(false)
    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _hasSearched = MutableStateFlow(false)
    private val _saveSearchFeedback = MutableStateFlow<String?>(null)

    private var activeSearchJob: Job? = null
    private var debounceJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoriteIdsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeFavoriteIds(auth.userScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchHistoryFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeSearchHistory(auth.userScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val savedSearchesFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeSavedSearches(auth.userScope)
    }

    val uiState: StateFlow<ExploreUiState> = combine(
        listOf(
            _filter,
            _minPriceText,
            _maxPriceText,
            _budgetError,
            marketplaceRepository.observeCategories(),
            marketplaceRepository.observeLocations(),
            _results,
            _nextCursor,
            _isLoading,
            _isLoadingMore,
            _isOfflineFallback,
            _lastSyncedAt,
            _errorMessage,
            searchHistoryFlow,
            savedSearchesFlow,
            _saveSearchFeedback,
            favoriteIdsFlow,
            localUserRepository.observeSettings(),
            _hasSearched
        )
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val currentResults = arr[6] as List<Listing>
        val currentFilter = arr[0] as SearchFilter
        val sponsoredPool = SponsoredRotationHelper.filterEligibleSponsored(currentResults)
            .let { pool ->
                if (!currentFilter.category.isNullOrBlank()) {
                    pool.filter { it.categoryId.equals(currentFilter.category, ignoreCase = true) }
                } else pool
            }

        @Suppress("UNCHECKED_CAST")
        ExploreUiState(
            filter = currentFilter,
            minPriceText = arr[1] as String,
            maxPriceText = arr[2] as String,
            budgetError = arr[3] as String?,
            categories = arr[4] as List<CategoryOption>,
            provinces = arr[5] as List<ProvinceLocations>,
            results = currentResults,
            sponsoredHighlights = sponsoredPool.take(6),
            nextCursor = arr[7] as String?,
            isLoading = arr[8] as Boolean,
            isLoadingMore = arr[9] as Boolean,
            isOfflineFallback = arr[10] as Boolean,
            lastSyncedAt = arr[11] as Long?,
            errorMessage = arr[12] as String?,
            searchHistory = arr[13] as List<String>,
            savedSearches = arr[14] as List<SavedSearch>,
            saveSearchFeedback = arr[15] as String?,
            favoriteIds = arr[16] as Set<String>,
            settings = arr[17] as AppSettings,
            hasSearched = arr[18] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ExploreUiState()
    )

    fun applyInitialCategory(categoryId: String?, initialQuery: String? = null) {
        val current = _filter.value
        val targetCat = categoryId?.takeIf { it.isNotBlank() }
        val targetQuery = initialQuery?.trim().orEmpty()
        if (targetCat == null && targetQuery.isBlank() && _hasSearched.value) return
        _filter.value = current.copy(
            category = targetCat ?: current.category,
            subcategory = if (targetCat != null && targetCat != current.category) null else current.subcategory,
            query = if (targetQuery.isNotBlank()) targetQuery else current.query
        )
        executeSearch(recordHistory = targetQuery.isNotBlank())
    }

    fun applySubcategoryFilter(categoryId: String, subcategory: String) {
        _filter.update {
            it.copy(
                category = categoryId,
                subcategory = subcategory
            )
        }
        executeSearch(recordHistory = false)
    }

    fun onQueryChanged(query: String) {
        _filter.update { it.copy(query = query) }
        debounceJob?.cancel()
        if (query.trim().length >= 2 || query.isEmpty()) {
            debounceJob = viewModelScope.launch {
                delay(380)
                if (_hasSearched.value || query.trim().length >= 2) {
                    executeSearch(recordHistory = false)
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _filter.update {
            it.copy(
                category = categoryId,
                subcategory = null
            )
        }
        executeSearch(recordHistory = false)
    }

    fun selectSubcategory(subcategory: String?) {
        _filter.update { it.copy(subcategory = subcategory) }
        executeSearch(recordHistory = false)
    }

    fun selectProvince(province: String?) {
        _filter.update {
            it.copy(
                province = province,
                district = if (province != it.province) null else it.district,
                town = if (province != it.province) null else it.town
            )
        }
        executeSearch(recordHistory = false)
    }

    fun selectDistrict(district: String?) {
        _filter.update { it.copy(district = district) }
        executeSearch(recordHistory = false)
    }

    fun selectTown(town: String?) {
        _filter.update { it.copy(town = town) }
        executeSearch(recordHistory = false)
    }

    fun onNearbyAreaChanged(area: String) {
        val clean = area.ifBlank { null }
        _filter.update { it.copy(nearbyArea = clean) }
        debounceJob?.cancel()
        if (_hasSearched.value || area.trim().length >= 2) {
            debounceJob = viewModelScope.launch {
                delay(380)
                executeSearch(recordHistory = false)
            }
        }
    }

    fun selectCondition(condition: String?) {
        _filter.update { it.copy(condition = condition) }
        executeSearch(recordHistory = false)
    }

    fun selectSort(sort: String) {
        _filter.update { it.copy(sort = sort) }
        executeSearch(recordHistory = false)
    }

    fun updateMinPrice(raw: String) {
        _minPriceText.value = raw
        revalidateAndUpdateBudget(minRaw = raw, maxRaw = _maxPriceText.value)
    }

    fun updateMaxPrice(raw: String) {
        _maxPriceText.value = raw
        revalidateAndUpdateBudget(minRaw = _minPriceText.value, maxRaw = raw)
    }

    fun clearBudgetFilter() {
        _minPriceText.value = ""
        _maxPriceText.value = ""
        _budgetError.value = null
        _filter.update { it.copy(minPrice = null, maxPrice = null) }
        if (_hasSearched.value) {
            executeSearch(recordHistory = false)
        }
    }

    private fun revalidateAndUpdateBudget(minRaw: String, maxRaw: String): Boolean {
        val minRes = PriceFormatter.parseStrictBudgetInput(minRaw)
        val maxRes = PriceFormatter.parseStrictBudgetInput(maxRaw)

        val err = when {
            minRes is BudgetParseResult.NegativeError || maxRes is BudgetParseResult.NegativeError ->
                "ZMW budget cannot be negative."
            minRes is BudgetParseResult.InvalidFormatError ->
                "Enter a valid minimum ZMW amount (e.g. 500 or 2,500.50)."
            maxRes is BudgetParseResult.InvalidFormatError ->
                "Enter a valid maximum ZMW amount (e.g. 5000 or 12,000)."
            minRes is BudgetParseResult.Valid && maxRes is BudgetParseResult.Valid && minRes.amount > maxRes.amount ->
                "Minimum budget cannot be greater than maximum budget."
            else -> null
        }
        _budgetError.value = err
        _filter.update {
            it.copy(
                minPrice = (minRes as? BudgetParseResult.Valid)?.amount,
                maxPrice = (maxRes as? BudgetParseResult.Valid)?.amount
            )
        }
        return err == null
    }

    fun clearFilters() {
        _filter.value = SearchFilter()
        _minPriceText.value = ""
        _maxPriceText.value = ""
        _budgetError.value = null
        _nextCursor.value = null
        _errorMessage.value = null
        _results.value = emptyList()
        _hasSearched.value = false
    }

    fun saveCurrentSearch(name: String, notifyMatches: Boolean = true) {
        val scope = authRepository.authState.value.userScope
        val current = _filter.value
        viewModelScope.launch {
            val saved = localUserRepository.saveSearch(
                userScope = scope,
                name = name,
                filter = current,
                notifyMatches = notifyMatches
            )
            _saveSearchFeedback.value = "Saved \"${saved.name}\""
        }
    }

    fun applySavedSearch(saved: SavedSearch) {
        val restored = saved.toSearchFilter()
        _filter.value = restored
        _minPriceText.value = restored.minPrice?.toLong()?.toString().orEmpty()
        _maxPriceText.value = restored.maxPrice?.toLong()?.toString().orEmpty()
        _budgetError.value = null
        executeSearch(recordHistory = false)
    }

    fun toggleSavedSearchAlerts(id: String, enabled: Boolean) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.toggleSavedSearchAlerts(scope, id, enabled)
        }
    }

    fun deleteSavedSearch(id: String) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.deleteSavedSearch(scope, id)
        }
    }

    fun clearSaveSearchFeedback() {
        _saveSearchFeedback.value = null
    }

    fun submitSearch() {
        debounceJob?.cancel()
        if (!revalidateAndUpdateBudget(_minPriceText.value, _maxPriceText.value)) {
            return
        }
        executeSearch(recordHistory = true)
    }

    fun useRecentSearch(query: String) {
        _filter.update { it.copy(query = query) }
        executeSearch(recordHistory = true)
    }

    fun removeSearchHistoryItem(query: String) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.removeSearchHistoryItem(scope, query)
        }
    }

    fun clearSearchHistory() {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.clearSearchHistory(scope)
        }
    }

    private fun executeSearch(recordHistory: Boolean) {
        if (!revalidateAndUpdateBudget(_minPriceText.value, _maxPriceText.value)) {
            return
        }
        activeSearchJob?.cancel()
        val filterSnapshot = _filter.value
        val requestKey = filterSnapshot.cacheKey()
        _hasSearched.value = true
        _nextCursor.value = null
        _errorMessage.value = null

        if (recordHistory && filterSnapshot.query.isNotBlank()) {
            val scope = authRepository.authState.value.userScope
            viewModelScope.launch {
                localUserRepository.addSearchHistory(scope, filterSnapshot.query)
            }
        }

        activeSearchJob = viewModelScope.launch {
            val cachedMatches = marketplaceRepository.searchCachedOfflineSnapshot(filterSnapshot)
            if (_filter.value.cacheKey() != requestKey) return@launch

            if (cachedMatches.isNotEmpty()) {
                _results.value = cachedMatches
                _isLoading.value = false
            } else {
                _results.value = emptyList()
                _isLoading.value = true
            }

            val res = marketplaceRepository.searchListings(filterSnapshot, cursor = null, limit = 20)
            // Ignore stale responses if filter changed mid-flight
            if (_filter.value.cacheKey() != requestKey) return@launch

            res.onSuccess { page ->
                _results.value = page.items.distinctBy { it.id }
                _nextCursor.value = page.nextCursor
                _isOfflineFallback.value = page.isOffline
                _lastSyncedAt.value = page.lastSyncedAt
                _errorMessage.value = null
            }.onFailure { err ->
                if (cachedMatches.isNotEmpty()) {
                    _results.value = cachedMatches
                    _isOfflineFallback.value = true
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = err.localizedMessage ?: "Search failed"
                }
            }
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        val cursor = _nextCursor.value ?: return
        if (_isLoading.value || _isLoadingMore.value) return
        val filterSnapshot = _filter.value
        val requestKey = filterSnapshot.cacheKey()

        _isLoadingMore.value = true
        viewModelScope.launch {
            val res = marketplaceRepository.searchListings(filterSnapshot, cursor = cursor, limit = 20)
            if (_filter.value.cacheKey() != requestKey) {
                _isLoadingMore.value = false
                return@launch
            }
            res.onSuccess { page ->
                val merged = (_results.value + page.items).distinctBy { it.id }
                _results.value = merged
                _nextCursor.value = page.nextCursor
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Could not load more results"
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
}
