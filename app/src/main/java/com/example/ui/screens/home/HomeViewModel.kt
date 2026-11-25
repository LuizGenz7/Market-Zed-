package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.HomepageEmptyMessages
import com.example.core.util.MotivationalMessage
import com.example.core.util.MotivationalMessageCatalog
import com.example.core.util.MotivationalMessageSelector
import com.example.data.auth.AuthRepository
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.AppSettings
import com.example.data.model.CachedFeedResult
import com.example.data.model.Listing
import com.example.data.model.SponsoredFormat
import com.example.data.model.SponsoredPlacementBanner
import com.example.data.repository.AccountRepository
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MutationResult
import com.example.data.repository.SponsoredRotationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val hasLoadedLocalCache: Boolean = false,
    val loadingSections: Set<String> = emptySet(),
    val carouselSponsored: List<Listing> = emptyList(),
    val homepageBanner: SponsoredPlacementBanner? = null,
    val sponsoredProducts: List<Listing> = emptyList(),
    val sponsoredDeals: List<Listing> = emptyList(),
    val recommendedProducts: List<Listing> = emptyList(),
    val recentlyAddedProducts: List<Listing> = emptyList(),
    val priceReducedProducts: List<Listing> = emptyList(),
    val popularSubcategories: List<MarketZedCatalogue.PopularProductSubcategory> = MarketZedCatalogue.popularProductSubcategories,
    val rentalProperties: List<Listing> = emptyList(),
    val sponsoredRentals: List<Listing> = emptyList(),
    val swapsPreview: List<Listing> = emptyList(),
    val servicesPreview: List<Listing> = emptyList(),
    val wantedPreview: List<Listing> = emptyList(),
    val promotedProducts: List<Listing> = emptyList(),
    val latestProducts: List<Listing> = emptyList(),
    val promotedJobs: List<Listing> = emptyList(),
    val latestJobs: List<Listing> = emptyList(),
    val recentlyViewed: List<Listing> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val isOffline: Boolean = false,
    val isStale: Boolean = false,
    val lastSyncedAt: Long? = null,
    val syncError: String? = null,
    val isSignedIn: Boolean = false,
    val testimonialSubmitting: Boolean = false,
    val testimonialFeedback: String? = null,
    val motivationalMessage: MotivationalMessage = MotivationalMessageCatalog.messages.first(),
    val emptyStateMessage: String = HomepageEmptyMessages.pickStableMessage()
) {
    val hasAnyMarketplaceContent: Boolean
        get() = carouselSponsored.isNotEmpty() ||
            homepageBanner != null ||
            sponsoredProducts.isNotEmpty() ||
            sponsoredDeals.isNotEmpty() ||
            recommendedProducts.isNotEmpty() ||
            recentlyAddedProducts.isNotEmpty() ||
            priceReducedProducts.isNotEmpty() ||
            rentalProperties.isNotEmpty() ||
            sponsoredRentals.isNotEmpty() ||
            swapsPreview.isNotEmpty() ||
            servicesPreview.isNotEmpty() ||
            wantedPreview.isNotEmpty() ||
            promotedJobs.isNotEmpty() ||
            latestJobs.isNotEmpty()
}

class HomeViewModel(
    private val marketplaceRepository: MarketplaceRepository,
    private val localUserRepository: LocalUserRepository,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val motivationalMessageSelector: MotivationalMessageSelector
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _isSyncingInFlight = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)
    private val _testimonialSubmitting = MutableStateFlow(false)
    private val _testimonialFeedback = MutableStateFlow<String?>(null)
    private val _motivationalMessage = MutableStateFlow(motivationalMessageSelector.nextMessage())
    private val stableEmptyStateMessage: String = HomepageEmptyMessages.pickStableMessage()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoriteIdsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeFavoriteIds(auth.userScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recentlyViewedFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeRecentlyViewed(auth.userScope)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        listOf(
            marketplaceRepository.observeSectionFeed(MarketplaceRepository.SECTION_HOME_PRODUCTS),
            marketplaceRepository.observeSectionFeed(MarketplaceRepository.SECTION_HOME_PROPERTY),
            marketplaceRepository.observeSectionFeed(MarketplaceRepository.SECTION_HOME_SERVICES),
            marketplaceRepository.observeSectionFeed(MarketplaceRepository.SECTION_HOME_SWAPS),
            marketplaceRepository.observeSectionFeed(MarketplaceRepository.SECTION_HOME_WANTED),
            marketplaceRepository.observeSectionFeed(MarketplaceRepository.SECTION_HOME_JOBS),
            marketplaceRepository.inFlightSections,
            favoriteIdsFlow,
            recentlyViewedFlow,
            localUserRepository.observeSettings(),
            authRepository.authState,
            _isRefreshing,
            _isSyncingInFlight,
            _syncError,
            _testimonialSubmitting,
            _testimonialFeedback,
            _motivationalMessage
        )
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val productsFeed = values[0] as CachedFeedResult<Listing>
        @Suppress("UNCHECKED_CAST")
        val propertyFeed = values[1] as CachedFeedResult<Listing>
        @Suppress("UNCHECKED_CAST")
        val servicesFeed = values[2] as CachedFeedResult<Listing>
        @Suppress("UNCHECKED_CAST")
        val swapsFeed = values[3] as CachedFeedResult<Listing>
        @Suppress("UNCHECKED_CAST")
        val wantedFeed = values[4] as CachedFeedResult<Listing>
        @Suppress("UNCHECKED_CAST")
        val jobsFeed = values[5] as CachedFeedResult<Listing>
        @Suppress("UNCHECKED_CAST")
        val inFlight = values[6] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val favIds = values[7] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val recentlyViewedList = values[8] as List<Listing>
        val settings = values[9] as AppSettings
        val auth = values[10] as com.example.data.auth.AuthUserState
        val refreshing = values[11] as Boolean
        val syncing = values[12] as Boolean
        val syncErr = values[13] as String?
        val testSubmitting = values[14] as Boolean
        val testFeedback = values[15] as String?
        val motivation = values[16] as MotivationalMessage

        val pureProducts = productsFeed.items.filter {
            !it.categoryId.equals("jobs", true) &&
                !it.categoryId.equals("services", true) &&
                !it.categoryId.equals("property", true) &&
                !it.isWanted
        }

        val allMarketplaceItems = (pureProducts + propertyFeed.items + swapsFeed.items + servicesFeed.items)
            .distinctBy { it.id }

        val allSponsored = SponsoredRotationHelper.filterEligibleSponsored(allMarketplaceItems)
        val carouselItems = SponsoredRotationHelper.rotateFairly(allSponsored, rotationSlot = 0, limit = 6)
        val heroIdExclude = carouselItems.take(1).map { it.id }.toSet()
        val bannerListing = SponsoredRotationHelper.rotateFairly(
            listings = allSponsored,
            rotationSlot = 1,
            excludeIds = heroIdExclude,
            limit = 1
        ).firstOrNull()
        val bannerModel = bannerListing?.let { item ->
            SponsoredPlacementBanner(
                listing = item,
                format = SponsoredFormat.HOMEPAGE_BANNER,
                headline = item.title,
                subheadline = "${MarketZedCatalogue.categoryLabel(item.categoryId)} · ${item.location?.displayTown ?: "Zambia"}"
            )
        }

        val sponsoredProd = SponsoredRotationHelper.rotateFairly(
            pureProducts,
            rotationSlot = 0,
            excludeIds = setOfNotNull(bannerListing?.id),
            limit = 10
        )
        val sponsoredDeals = SponsoredRotationHelper.rotateFairly(
            allSponsored.filter { it.campaignFormat == SponsoredFormat.SPONSORED_DEAL.wireValue || it.isPromoted },
            rotationSlot = 2,
            limit = 8
        )

        val organicProd = pureProducts.filter { !it.isPromoted }
        val recentlyAdded = pureProducts.sortedByDescending { it.createdAtMillis }.take(12)
        val priceReduced = allMarketplaceItems
            .filter { it.hasVerifiedPriceDrop }
            .sortedByDescending { it.priceDropPercent ?: 0 }
            .take(10)

        val sponsoredRent = SponsoredRotationHelper.filterEligibleSponsored(propertyFeed.items)
        val organicRent = propertyFeed.items.filter { !it.isPromoted }

        val swapsOrdered = SponsoredRotationHelper.prioritizeSponsoredInFeed(swapsFeed.items).take(8)
        val servicesOrdered = SponsoredRotationHelper.prioritizeSponsoredInFeed(servicesFeed.items).take(5)

        val promotedJobs = jobsFeed.items.filter { it.isPromoted }
        val latestJobs = jobsFeed.items.filter { !it.isPromoted }.take(5)

        HomeUiState(
            isRefreshing = refreshing,
            isSyncing = syncing,
            hasLoadedLocalCache = true,
            loadingSections = inFlight,
            carouselSponsored = carouselItems,
            homepageBanner = bannerModel,
            sponsoredProducts = sponsoredProd,
            sponsoredDeals = sponsoredDeals,
            recommendedProducts = (sponsoredProd.take(2) + organicProd).distinctBy { it.id }.take(14),
            recentlyAddedProducts = recentlyAdded,
            priceReducedProducts = priceReduced,
            rentalProperties = organicRent.take(10),
            sponsoredRentals = sponsoredRent.take(6),
            swapsPreview = swapsOrdered,
            servicesPreview = servicesOrdered,
            wantedPreview = wantedFeed.items.take(3),
            promotedProducts = sponsoredProd,
            latestProducts = organicProd,
            promotedJobs = promotedJobs,
            latestJobs = latestJobs,
            recentlyViewed = recentlyViewedList.filter { it.isEligiblePublic }.take(10),
            favoriteIds = favIds,
            settings = settings,
            isOffline = productsFeed.isOffline,
            isStale = productsFeed.isStale,
            lastSyncedAt = productsFeed.lastSyncedAt,
            syncError = syncErr ?: if (productsFeed.items.isEmpty()) productsFeed.errorMessage else null,
            isSignedIn = auth.isSignedIn,
            testimonialSubmitting = testSubmitting,
            testimonialFeedback = testFeedback,
            motivationalMessage = motivation,
            emptyStateMessage = stableEmptyStateMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState()
    )

    init {
        refresh(force = true, showIndicator = false)
    }

    fun rotateMotivationalMessage() {
        val currentId = _motivationalMessage.value.id
        _motivationalMessage.value = motivationalMessageSelector.nextMessage(excludingId = currentId)
    }

    fun refresh(force: Boolean = true, showIndicator: Boolean = force) {
        if (_isSyncingInFlight.value) return
        viewModelScope.launch {
            _isSyncingInFlight.value = true
            if (showIndicator) {
                _isRefreshing.value = true
            }
            _syncError.value = null
            val res = marketplaceRepository.refreshHomeFeeds(force = force)
            res.onFailure { err ->
                if (showIndicator || uiState.value.latestProducts.isEmpty()) {
                    _syncError.value = err.localizedMessage ?: "Unable to sync latest listings"
                }
            }
            _isRefreshing.value = false
            _isSyncingInFlight.value = false
        }
    }

    fun clearRecentlyViewed() {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.clearRecentlyViewed(scope)
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun toggleFavorite(listingId: String) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.toggleFavorite(scope, listingId)
        }
    }

    fun toggleDataSaver() {
        viewModelScope.launch {
            localUserRepository.updateSettings { it.copy(dataSaverEnabled = !it.dataSaverEnabled) }
        }
    }

    fun submitTestimonial(name: String, town: String, message: String, rating: Int) {
        if (name.trim().length < 2) {
            _testimonialFeedback.value = "Name must be at least 2 characters."
            return
        }
        if (message.trim().length < 12) {
            _testimonialFeedback.value = "Please share at least 12 characters of feedback."
            return
        }
        viewModelScope.launch {
            _testimonialSubmitting.value = true
            _testimonialFeedback.value = null
            when (val res = accountRepository.submitTestimonial(name, town, message, rating)) {
                is MutationResult.Success ->
                    _testimonialFeedback.value = "Thank you! Your testimonial was submitted for review before appearing publicly."
                is MutationResult.Error ->
                    _testimonialFeedback.value = res.message
                is MutationResult.AmbiguousTimeout ->
                    _testimonialFeedback.value = res.message
            }
            _testimonialSubmitting.value = false
        }
    }

    fun clearTestimonialFeedback() {
        _testimonialFeedback.value = null
    }
}
