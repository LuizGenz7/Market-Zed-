package com.example.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.AdminOverview
import com.example.data.model.AdminPaymentRecord
import com.example.data.model.AdminPromotionRecord
import com.example.data.model.AdminReportRecord
import com.example.data.model.AdminUserRecord
import com.example.data.model.Listing
import com.example.data.model.PromotionPricing
import com.example.data.model.PromotionalBonusRule
import com.example.data.repository.AdminRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MutationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    LISTINGS("Listings"),
    PROMOTIONS("Promotions"),
    REPORTS("Reports"),
    USERS("Users"),
    PAYMENTS("Payments")
}

data class AdminUiState(
    val selectedTab: AdminTab = AdminTab.OVERVIEW,
    val overview: AdminOverview? = null,
    val overviewLoading: Boolean = false,
    val overviewError: String? = null,
    val listings: List<Listing> = emptyList(),
    val listingsLoading: Boolean = false,
    val listingsError: String? = null,
    val listingStatusFilter: String = "all",
    val listingQuery: String = "",
    val promotions: List<AdminPromotionRecord> = emptyList(),
    val promotionPricing: PromotionPricing? = null,
    val promotionsLoading: Boolean = false,
    val promotionsError: String? = null,
    val promotionStatusFilter: String = "all",
    val reports: List<AdminReportRecord> = emptyList(),
    val reportsLoading: Boolean = false,
    val reportsError: String? = null,
    val reportStatusFilter: String = "open",
    val users: List<AdminUserRecord> = emptyList(),
    val usersLoading: Boolean = false,
    val usersError: String? = null,
    val userFilter: String = "all",
    val userQuery: String = "",
    val payments: List<AdminPaymentRecord> = emptyList(),
    val paymentsLoading: Boolean = false,
    val paymentsError: String? = null,
    val paymentStatusFilter: String = "all",
    val bonusRules: List<PromotionalBonusRule> = emptyList(),
    val actionBusy: Boolean = false,
    val actionSuccess: String? = null,
    val actionError: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val marketplaceRepository: MarketplaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun selectTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            AdminTab.OVERVIEW -> if (_uiState.value.overview == null) loadOverview()
            AdminTab.LISTINGS -> if (_uiState.value.listings.isEmpty()) loadListings()
            AdminTab.PROMOTIONS -> if (_uiState.value.promotions.isEmpty() || _uiState.value.promotionPricing == null) loadPromotions()
            AdminTab.REPORTS -> if (_uiState.value.reports.isEmpty()) loadReports()
            AdminTab.USERS -> if (_uiState.value.users.isEmpty()) loadUsers()
            AdminTab.PAYMENTS -> if (_uiState.value.payments.isEmpty()) loadPayments()
        }
    }

    fun refreshCurrentTab() {
        when (_uiState.value.selectedTab) {
            AdminTab.OVERVIEW -> loadOverview()
            AdminTab.LISTINGS -> loadListings()
            AdminTab.PROMOTIONS -> loadPromotions()
            AdminTab.REPORTS -> loadReports()
            AdminTab.USERS -> loadUsers()
            AdminTab.PAYMENTS -> loadPayments()
        }
    }

    fun loadAllInitial() {
        loadOverview()
    }

    fun loadOverview() {
        viewModelScope.launch {
            _uiState.update { it.copy(overviewLoading = true, overviewError = null) }
            adminRepository.getOverview()
                .onSuccess { data ->
                    _uiState.update { it.copy(overview = data, overviewLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            overviewLoading = false,
                            overviewError = err.localizedMessage ?: "Failed to load admin overview."
                        )
                    }
                }
        }
    }

    fun setListingFilter(status: String) {
        _uiState.update { it.copy(listingStatusFilter = status) }
        loadListings()
    }

    fun setListingQuery(query: String) {
        _uiState.update { it.copy(listingQuery = query) }
    }

    fun loadListings() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(listingsLoading = true, listingsError = null) }
            adminRepository.getListings(
                status = current.listingStatusFilter,
                query = current.listingQuery
            )
                .onSuccess { items ->
                    _uiState.update { it.copy(listings = items, listingsLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            listingsLoading = false,
                            listingsError = err.localizedMessage ?: "Failed to load listings queue."
                        )
                    }
                }
        }
    }

    fun moderateListing(listingId: String, action: String, reason: String? = null) {
        if (action in setOf("approve", "restore", "promote")) {
            val target = _uiState.value.listings.firstOrNull { it.id == listingId }
            if (target != null) {
                val policyViolation = MarketZedCatalogue.validatePropertyAndLandPolicy(
                    categoryId = target.categoryId,
                    subcategory = target.subcategory,
                    title = target.title,
                    description = target.description,
                    priceMode = target.priceMode
                )
                if (policyViolation != null) {
                    _uiState.update { it.copy(actionError = "Policy violation: $policyViolation") }
                    return
                }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccess = null) }
            when (val res = adminRepository.moderateListing(listingId, action, reason)) {
                is MutationResult.Success -> {
                    val label = when (action) {
                        "approve" -> "Listing approved"
                        "hide" -> "Listing hidden"
                        "restore" -> "Listing restored"
                        "reject", "remove" -> "Listing removed"
                        "promote" -> "Listing sponsored"
                        "unpromote" -> "Sponsored placement removed"
                        else -> "Listing updated"
                    }
                    _uiState.update { it.copy(actionBusy = false, actionSuccess = label) }
                    loadListings()
                    loadOverview()
                    marketplaceRepository.refreshHomeFeeds(force = true)
                }
                is MutationResult.Error -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
                is MutationResult.AmbiguousTimeout -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
            }
        }
    }

    fun setPromotionFilter(status: String) {
        _uiState.update { it.copy(promotionStatusFilter = status) }
        loadPromotions()
    }

    fun loadPromotions() {
        val status = _uiState.value.promotionStatusFilter
        viewModelScope.launch {
            _uiState.update { it.copy(promotionsLoading = true, promotionsError = null) }
            adminRepository.getPromotionPricing()
                .onSuccess { pricing ->
                    _uiState.update { it.copy(promotionPricing = pricing) }
                }
            adminRepository.getPromotions(status = status)
                .onSuccess { items ->
                    _uiState.update { it.copy(promotions = items, promotionsLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            promotionsLoading = false,
                            promotionsError = err.localizedMessage ?: "Failed to load promotions."
                        )
                    }
                }
        }
    }

    fun managePromotion(
        promotionId: String,
        action: String,
        durationDays: Int? = null,
        reason: String? = null,
        campaignFormat: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccess = null) }
            when (val res = adminRepository.managePromotion(promotionId, action, durationDays, reason, campaignFormat)) {
                is MutationResult.Success -> {
                    _uiState.update { it.copy(actionBusy = false, actionSuccess = "Promotion updated ($action).") }
                    loadPromotions()
                    loadOverview()
                    marketplaceRepository.refreshHomeFeeds(force = true)
                }
                is MutationResult.Error -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
                is MutationResult.AmbiguousTimeout -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
            }
        }
    }

    fun grantPromotion(
        listingId: String,
        durationDays: Int,
        note: String?,
        campaignFormat: String? = null
    ) {
        if (listingId.isBlank()) {
            _uiState.update { it.copy(actionError = "Enter a valid listing ID.") }
            return
        }
        val target = _uiState.value.listings.firstOrNull { it.id == listingId.trim() }
        if (target != null) {
            val policyViolation = MarketZedCatalogue.validatePropertyAndLandPolicy(
                categoryId = target.categoryId,
                subcategory = target.subcategory,
                title = target.title,
                description = target.description,
                priceMode = target.priceMode
            )
            if (policyViolation != null) {
                _uiState.update { it.copy(actionError = "Policy violation: $policyViolation") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccess = null) }
            when (val res = adminRepository.grantPromotion(listingId, durationDays, note, campaignFormat)) {
                is MutationResult.Success -> {
                    _uiState.update { it.copy(actionBusy = false, actionSuccess = "Sponsored listing for ${durationDays}d.") }
                    loadPromotions()
                    loadOverview()
                    marketplaceRepository.refreshHomeFeeds(force = true)
                }
                is MutationResult.Error -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
                is MutationResult.AmbiguousTimeout -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
            }
        }
    }

    fun updatePromotionPricing(price7d: Double?, price14d: Double?, price30d: Double?) {
        if (price7d == null || price14d == null || price30d == null ||
            price7d <= 0.0 || price14d <= 0.0 || price30d <= 0.0
        ) {
            _uiState.update { it.copy(actionError = "Enter valid positive ZMW prices for 7, 14, and 30 days.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccess = null) }
            val payload = mapOf("7" to price7d, "14" to price14d, "30" to price30d)
            when (val res = adminRepository.updatePromotionPricing(payload)) {
                is MutationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            promotionPricing = res.data,
                            actionSuccess = "Promotion pricing updated."
                        )
                    }
                }
                is MutationResult.Error -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
                is MutationResult.AmbiguousTimeout -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
            }
        }
    }

    fun setReportFilter(status: String) {
        _uiState.update { it.copy(reportStatusFilter = status) }
        loadReports()
    }

    fun loadReports() {
        val status = _uiState.value.reportStatusFilter
        viewModelScope.launch {
            _uiState.update { it.copy(reportsLoading = true, reportsError = null) }
            adminRepository.getReports(status = status)
                .onSuccess { items ->
                    _uiState.update { it.copy(reports = items, reportsLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            reportsLoading = false,
                            reportsError = err.localizedMessage ?: "Failed to load reports."
                        )
                    }
                }
        }
    }

    fun resolveReport(reportId: String, action: String, resolutionNote: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccess = null) }
            when (val res = adminRepository.resolveReport(reportId, action, resolutionNote)) {
                is MutationResult.Success -> {
                    _uiState.update { it.copy(actionBusy = false, actionSuccess = "Report updated.") }
                    loadReports()
                    loadListings()
                    loadOverview()
                    marketplaceRepository.refreshHomeFeeds(force = true)
                }
                is MutationResult.Error -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
                is MutationResult.AmbiguousTimeout -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
            }
        }
    }

    fun setUserFilter(filter: String) {
        _uiState.update { it.copy(userFilter = filter) }
        loadUsers()
    }

    fun setUserQuery(query: String) {
        _uiState.update { it.copy(userQuery = query) }
    }

    fun loadUsers() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(usersLoading = true, usersError = null) }
            adminRepository.getUsers(query = current.userQuery, filter = current.userFilter)
                .onSuccess { items ->
                    _uiState.update { it.copy(users = items, usersLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            usersLoading = false,
                            usersError = err.localizedMessage ?: "Failed to load users."
                        )
                    }
                }
        }
    }

    fun manageUser(uid: String, action: String, reason: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccess = null) }
            when (val res = adminRepository.manageUser(uid, action, reason)) {
                is MutationResult.Success -> {
                    _uiState.update { it.copy(actionBusy = false, actionSuccess = "Account updated ($action).") }
                    loadUsers()
                    loadOverview()
                }
                is MutationResult.Error -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
                is MutationResult.AmbiguousTimeout -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
            }
        }
    }

    fun setPaymentFilter(status: String) {
        _uiState.update { it.copy(paymentStatusFilter = status) }
        loadPayments()
    }

    fun loadPayments() {
        val status = _uiState.value.paymentStatusFilter
        viewModelScope.launch {
            _uiState.update { it.copy(paymentsLoading = true, paymentsError = null) }
            adminRepository.getPayments(status = status)
                .onSuccess { items ->
                    _uiState.update { it.copy(payments = items, paymentsLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            paymentsLoading = false,
                            paymentsError = err.localizedMessage ?: "Failed to load payments."
                        )
                    }
                }
        }
    }

    fun sendBroadcast(title: String, message: String, categoryId: String? = null, listingId: String? = null) {
        if (title.isBlank() || message.isBlank()) {
            _uiState.update { it.copy(actionError = "Enter both a broadcast title and message.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccess = null) }
            when (val res = adminRepository.sendBroadcast(title, message, categoryId, listingId)) {
                is MutationResult.Success -> {
                    _uiState.update { it.copy(actionBusy = false, actionSuccess = "Broadcast sent to MarketZed members.") }
                    loadOverview()
                }
                is MutationResult.Error -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
                is MutationResult.AmbiguousTimeout -> _uiState.update { it.copy(actionBusy = false, actionError = res.message) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(actionSuccess = null, actionError = null) }
    }

    fun clearState() {
        _uiState.value = AdminUiState()
    }
}
