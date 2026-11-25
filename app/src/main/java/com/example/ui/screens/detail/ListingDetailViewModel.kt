package com.example.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.model.AppSettings
import com.example.data.model.Listing
import com.example.data.repository.AccountRepository
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MutationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListingDetailUiState(
    val listing: Listing? = null,
    val similarProducts: List<Listing> = emptyList(),
    val isFavorite: Boolean = false,
    val isSignedIn: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val reportSubmitting: Boolean = false,
    val reportResultMessage: String? = null
)

class ListingDetailViewModel(
    private val marketplaceRepository: MarketplaceRepository,
    private val localUserRepository: LocalUserRepository,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _listingId = MutableStateFlow<String?>(null)
    private val _similarProducts = MutableStateFlow<List<Listing>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _reportSubmitting = MutableStateFlow(false)
    private val _reportResultMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val listingFlow = _listingId.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(null) else marketplaceRepository.observeListingById(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoriteIdsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeFavoriteIds(auth.userScope)
    }

    val uiState: StateFlow<ListingDetailUiState> = combine(
        listOf(
            listingFlow,
            _similarProducts,
            favoriteIdsFlow,
            authRepository.authState,
            localUserRepository.observeSettings(),
            _isLoading,
            _errorMessage,
            _reportSubmitting,
            _reportResultMessage
        )
    ) { arr ->
        val listing = arr[0] as Listing?
        @Suppress("UNCHECKED_CAST")
        val similar = arr[1] as List<Listing>
        @Suppress("UNCHECKED_CAST")
        val favIds = arr[2] as Set<String>
        val auth = arr[3] as com.example.data.auth.AuthUserState
        val settings = arr[4] as AppSettings
        val loading = arr[5] as Boolean
        val error = arr[6] as String?
        val reporting = arr[7] as Boolean
        val reportMsg = arr[8] as String?

        ListingDetailUiState(
            listing = listing,
            similarProducts = similar,
            isFavorite = listing?.id in favIds,
            isSignedIn = auth.isSignedIn,
            settings = settings,
            isLoading = loading && listing == null,
            errorMessage = if (listing == null && !loading) error ?: "Listing not found in cache or server." else null,
            reportSubmitting = reporting,
            reportResultMessage = reportMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListingDetailUiState()
    )

    fun loadListing(listingId: String) {
        if (_listingId.value == listingId && uiState.value.listing != null) return
        _listingId.value = listingId
        _isLoading.value = true
        _errorMessage.value = null
        _similarProducts.value = emptyList()

        viewModelScope.launch {
            val scope = authRepository.authState.value.userScope
            val cached = marketplaceRepository.getCachedListingById(listingId)
            if (cached != null) {
                localUserRepository.recordRecentlyViewed(scope, listingId)
                _isLoading.value = false
                loadSimilarListings(cached)
            } else {
                val res = marketplaceRepository.searchListings(
                    filter = com.example.data.model.SearchFilter(query = listingId),
                    limit = 5
                )
                val match = res.getOrNull()?.items?.firstOrNull { it.id == listingId }
                if (match != null) {
                    marketplaceRepository.upsertListingSnapshot(match)
                    localUserRepository.recordRecentlyViewed(scope, listingId)
                    loadSimilarListings(match)
                } else if (res.isFailure) {
                    _errorMessage.value = res.exceptionOrNull()?.localizedMessage
                }
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSimilarListings(anchor: Listing) {
        val cachedMatches = marketplaceRepository.searchCachedOfflineSnapshot(
            com.example.data.model.SearchFilter(category = anchor.categoryId)
        )
            .filter { it.id != anchor.id && it.isEligiblePublic }
            .sortedWith(
                compareByDescending<Listing> { it.subcategory.equals(anchor.subcategory, ignoreCase = true) }
                    .thenByDescending { it.location?.province.equals(anchor.location?.province, ignoreCase = true) }
                    .thenByDescending { it.isPromoted }
                    .thenByDescending { it.createdAtMillis }
            )
            .take(6)

        if (cachedMatches.isNotEmpty()) {
            _similarProducts.value = cachedMatches
            return
        }

        val liveRes = marketplaceRepository.searchListings(
            filter = com.example.data.model.SearchFilter(
                category = anchor.categoryId,
                subcategory = anchor.subcategory.takeIf { it.isNotBlank() }
            ),
            limit = 8
        )
        liveRes.onSuccess { feed ->
            _similarProducts.value = feed.items
                .filter { it.id != anchor.id && it.isEligiblePublic }
                .take(6)
        }
    }

    fun toggleFavorite() {
        val id = _listingId.value ?: return
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.toggleFavorite(scope, id)
        }
    }

    fun submitReport(reason: String, details: String) {
        val id = _listingId.value ?: return
        viewModelScope.launch {
            _reportSubmitting.value = true
            _reportResultMessage.value = null
            when (val res = accountRepository.reportListing(id, reason, details)) {
                is MutationResult.Success ->
                    _reportResultMessage.value = "Thank you. Your report has been submitted for review."
                is MutationResult.Error ->
                    _reportResultMessage.value = res.message
                is MutationResult.AmbiguousTimeout ->
                    _reportResultMessage.value = res.message
            }
            _reportSubmitting.value = false
        }
    }

    fun clearReportFeedback() {
        _reportResultMessage.value = null
    }
}
