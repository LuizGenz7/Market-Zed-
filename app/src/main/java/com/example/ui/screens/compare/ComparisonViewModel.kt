package com.example.ui.screens.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.DateUtils
import com.example.core.util.PriceFormatter
import com.example.data.auth.AuthRepository
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.repository.ComparisonAddResult
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ComparisonViewModel(
    private val marketplaceRepository: MarketplaceRepository,
    private val localUserRepository: LocalUserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<ComparisonItemState>>(emptyList())
    private val _showDifferencesOnly = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _isSheetOpen = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _showLimitAlert = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedIdsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeComparisonIds(auth.userScope)
    }

    init {
        viewModelScope.launch {
            selectedIdsFlow.collectLatest { ids ->
                refreshComparisonItems(ids)
            }
        }
    }

    val uiState: StateFlow<ComparisonUiState> = combine(
        listOf(
            selectedIdsFlow,
            _items,
            _showDifferencesOnly,
            _isLoading,
            _isSheetOpen,
            _feedbackMessage,
            _showLimitAlert
        )
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val ids = arr[0] as List<String>
        @Suppress("UNCHECKED_CAST")
        val items = arr[1] as List<ComparisonItemState>
        val diffOnly = arr[2] as Boolean
        val loading = arr[3] as Boolean
        val sheetOpen = arr[4] as Boolean
        val feedback = arr[5] as String?
        val limitAlert = arr[6] as Boolean

        val rows = buildComparisonRows(items)
        val differing = rows.filter { it.hasDifference }.map { it.attribute }.toSet()

        ComparisonUiState(
            selectedIds = ids,
            items = items,
            specRows = if (diffOnly) rows.filter { it.hasDifference } else rows,
            differingAttributes = differing,
            showDifferencesOnly = diffOnly,
            isLoading = loading,
            isSheetOpen = sheetOpen,
            feedbackMessage = feedback,
            showLimitAlert = limitAlert
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ComparisonUiState()
    )

    private suspend fun refreshComparisonItems(ids: List<String>) {
        if (ids.isEmpty()) {
            _items.value = emptyList()
            _isLoading.value = false
            return
        }
        _isLoading.value = true
        val resolved = marketplaceRepository.getListingsForComparison(ids)
        _items.value = ids.map { id ->
            val listing = resolved[id]
            when {
                listing == null -> ComparisonItemState(
                    listingId = id,
                    listing = null,
                    isUnavailable = true,
                    unavailableReason = "Listing removed or unavailable"
                )
                !listing.isEligiblePublic || DateUtils.isExpired(listing.expiresAt) -> ComparisonItemState(
                    listingId = id,
                    listing = listing,
                    isUnavailable = true,
                    unavailableReason = "Expired or no longer available"
                )
                else -> ComparisonItemState(
                    listingId = id,
                    listing = listing,
                    isUnavailable = false
                )
            }
        }
        _isLoading.value = false
    }

    fun toggleCompare(listing: Listing) {
        if (!listing.isComparableCategory) {
            _feedbackMessage.value = "Comparison is available for products, rental properties, swaps, and free items."
            return
        }
        val scope = authRepository.authState.value.userScope
        val currentlySelected = uiState.value.selectedIdSet.contains(listing.id)
        viewModelScope.launch {
            if (currentlySelected) {
                localUserRepository.removeComparisonItem(scope, listing.id)
                _feedbackMessage.value = "Removed \"${listing.title}\" from comparison."
            } else {
                when (localUserRepository.addComparisonItem(scope, listing.id)) {
                    ComparisonAddResult.Added -> {
                        _feedbackMessage.value = "Added \"${listing.title}\" to comparison."
                    }
                    ComparisonAddResult.AlreadySelected -> {
                        _feedbackMessage.value = "\"${listing.title}\" is already in comparison."
                    }
                    ComparisonAddResult.LimitReached -> {
                        _showLimitAlert.value = true
                        _feedbackMessage.value = "You can compare up to 3 products at a time. Remove one product first."
                    }
                }
            }
        }
    }

    fun removeItem(listingId: String) {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.removeComparisonItem(scope, listingId)
        }
    }

    fun clearAll() {
        val scope = authRepository.authState.value.userScope
        viewModelScope.launch {
            localUserRepository.clearComparisonItems(scope)
            _isSheetOpen.value = false
        }
    }

    fun openQuickSheet() {
        _isSheetOpen.value = true
    }

    fun dismissQuickSheet() {
        _isSheetOpen.value = false
    }

    fun setShowDifferencesOnly(onlyDiffs: Boolean) {
        _showDifferencesOnly.value = onlyDiffs
    }

    fun dismissFeedback() {
        _feedbackMessage.value = null
        _showLimitAlert.value = false
    }

    fun buildShareSummary(): String {
        val current = uiState.value
        if (current.items.isEmpty()) return "MarketZed Product Comparison"
        return buildString {
            appendLine("MarketZed Product Comparison (${current.items.size}/3)")
            appendLine()
            current.items.forEachIndexed { idx, item ->
                val l = item.listing
                if (l == null || item.isUnavailable) {
                    appendLine("${idx + 1}. Listing unavailable (${item.listingId})")
                } else {
                    val priceText = PriceFormatter.formatListingPrice(l.price, l.priceMode, l.categoryId)
                    appendLine("${idx + 1}. ${l.title} — $priceText (${l.location?.displayFull ?: "Zambia"})")
                }
            }
            val allRows = buildComparisonRows(current.items)
            if (allRows.isNotEmpty()) {
                appendLine()
                allRows.forEach { row ->
                    appendLine("${row.attribute}: ${row.values.joinToString(" | ")}")
                }
            }
        }.trim()
    }

    companion object {
        fun buildComparisonRows(items: List<ComparisonItemState>): List<ComparisonSpecRow> {
            if (items.isEmpty()) return emptyList()
            val availableListings = items.mapNotNull { it.listing }
            if (availableListings.isEmpty()) return emptyList()

            val unifiedSpecKeys = linkedSetOf<String>()
            availableListings.forEach { listing ->
                MarketZedCatalogue.expectedSpecKeysForListing(listing).forEach { unifiedSpecKeys += it }
                listing.resolvedSpecifications.keys.forEach { k ->
                    if (unifiedSpecKeys.none { it.equals(k, ignoreCase = true) }) {
                        unifiedSpecKeys += k
                    }
                }
            }

            val rows = mutableListOf<ComparisonSpecRow>()

            fun addRow(attribute: String, extractor: (ComparisonItemState) -> String) {
                val values = items.map(extractor)
                val distinctComparable = values.filter { it != "Unavailable" }.map { it.trim().lowercase() }.distinct()
                val hasDiff = items.size > 1 && distinctComparable.size > 1
                rows += ComparisonSpecRow(
                    attribute = attribute,
                    values = values,
                    hasDifference = hasDiff
                )
            }

            addRow("Price") { item ->
                val l = item.listing
                if (l == null || item.isUnavailable) "Unavailable"
                else PriceFormatter.formatListingPrice(l.price, l.priceMode, l.categoryId)
            }
            addRow("Category") { item ->
                val l = item.listing
                if (l == null || item.isUnavailable) "Unavailable"
                else buildString {
                    append(MarketZedCatalogue.categoryLabel(l.categoryId))
                    if (l.subcategory.isNotBlank()) append(" · ${l.subcategory}")
                }
            }
            addRow("Location") { item ->
                val l = item.listing
                if (l == null || item.isUnavailable) "Unavailable"
                else l.location?.displayFull ?: "Not provided"
            }
            addRow("Seller") { item ->
                val l = item.listing
                if (l == null || item.isUnavailable) "Unavailable"
                else buildString {
                    append(l.sellerName?.takeIf { it.isNotBlank() } ?: "Not provided")
                    if (l.sellerVerified) append(" (Verified)")
                }
            }
            addRow("Availability") { item ->
                val l = item.listing
                when {
                    l == null || item.isUnavailable -> item.unavailableReason ?: "Unavailable"
                    !l.availabilityStatus.isNullOrBlank() -> l.availabilityStatus.replaceFirstChar { it.uppercase() }
                    else -> "Available"
                }
            }
            addRow("Listed Date") { item ->
                val l = item.listing
                if (l == null || item.isUnavailable) "Unavailable"
                else if (l.createdAtMillis > 0L) DateUtils.formatDateLusaka(l.createdAtMillis) else "Not provided"
            }

            val extractedByListingId = availableListings.associate { l ->
                l.id to MarketZedCatalogue.extractComparisonSpecs(l, unifiedSpecKeys.toList())
            }
            unifiedSpecKeys.forEach { specKey ->
                addRow(specKey) { item ->
                    val l = item.listing
                    if (l == null || item.isUnavailable) {
                        "Unavailable"
                    } else {
                        extractedByListingId[l.id]?.get(specKey)?.takeIf { it.isNotBlank() } ?: "Not provided"
                    }
                }
            }

            return rows
        }
    }
}
