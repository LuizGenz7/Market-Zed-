package com.example.ui.screens.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.AppSettings
import com.example.data.model.SellerPublicProfile
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SellerProfileUiState(
    val profile: SellerPublicProfile? = null,
    val selectedCategoryId: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
) {
    val availableCategoryIds: List<String>
        get() = profile?.listings.orEmpty()
            .map { it.categoryId }
            .distinct()

    val filteredListings: List<com.example.data.model.Listing>
        get() {
            val all = profile?.listings.orEmpty()
            val cat = selectedCategoryId ?: return all
            return all.filter { it.categoryId.equals(cat, ignoreCase = true) }
        }
}

class SellerProfileViewModel(
    private val marketplaceRepository: MarketplaceRepository,
    private val localUserRepository: LocalUserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sellerKey = MutableStateFlow<String?>(null)
    private val _anchorListing = MutableStateFlow<com.example.data.model.Listing?>(null)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sellerListingsFlow = _sellerKey.flatMapLatest { key ->
        if (key.isNullOrBlank()) flowOf(emptyList()) else marketplaceRepository.observeSellerListings(key)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoriteIdsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeFavoriteIds(auth.userScope)
    }

    val uiState: StateFlow<SellerProfileUiState> = combine(
        listOf(
            _sellerKey,
            _anchorListing,
            sellerListingsFlow,
            _selectedCategoryId,
            favoriteIdsFlow,
            localUserRepository.observeSettings(),
            _isLoading
        )
    ) { arr ->
        val sellerKey = arr[0] as String?
        val anchor = arr[1] as com.example.data.model.Listing?
        @Suppress("UNCHECKED_CAST")
        val listings = arr[2] as List<com.example.data.model.Listing>
        val selectedCat = arr[3] as String?
        @Suppress("UNCHECKED_CAST")
        val favIds = arr[4] as Set<String>
        val settings = arr[5] as AppSettings
        val loading = arr[6] as Boolean

        val profile = if (!sellerKey.isNullOrBlank()) {
            marketplaceRepository.buildSellerPublicProfile(
                sellerKey = sellerKey,
                listings = listings,
                anchorListing = anchor
            )
        } else {
            null
        }

        SellerProfileUiState(
            profile = profile,
            selectedCategoryId = selectedCat?.takeIf { c ->
                profile?.listings?.any { it.categoryId.equals(c, ignoreCase = true) } == true
            },
            favoriteIds = favIds,
            settings = settings,
            isLoading = loading && profile == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SellerProfileUiState()
    )

    fun loadSeller(sellerKey: String, anchorListingId: String? = null) {
        _sellerKey.value = sellerKey
        _selectedCategoryId.value = null
        _isLoading.value = true
        viewModelScope.launch {
            val anchor = anchorListingId?.takeIf { it.isNotBlank() }?.let {
                marketplaceRepository.getCachedListingById(it)
            }
            _anchorListing.value = anchor
            _isLoading.value = false
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun toggleFavorite(listingId: String) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.toggleFavorite(scope, listingId)
        }
    }

    fun categoryTabLabel(categoryId: String): String =
        MarketZedCatalogue.categoryLabel(categoryId)
}
