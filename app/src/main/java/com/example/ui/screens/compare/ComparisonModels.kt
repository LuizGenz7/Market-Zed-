package com.example.ui.screens.compare

import com.example.data.model.Listing

data class ComparisonSpecRow(
    val attribute: String,
    val values: List<String>,
    val hasDifference: Boolean
)

data class ComparisonItemState(
    val listingId: String,
    val listing: Listing?,
    val isUnavailable: Boolean,
    val unavailableReason: String? = null
)

data class ComparisonUiState(
    val selectedIds: List<String> = emptyList(),
    val items: List<ComparisonItemState> = emptyList(),
    val specRows: List<ComparisonSpecRow> = emptyList(),
    val differingAttributes: Set<String> = emptySet(),
    val showDifferencesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val isSheetOpen: Boolean = false,
    val feedbackMessage: String? = null,
    val showLimitAlert: Boolean = false
) {
    val selectedIdSet: Set<String>
        get() = selectedIds.toSet()

    val count: Int
        get() = selectedIds.size
}
