package com.example.ui.screens.compare

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.CloudinaryUrlHelper
import com.example.core.util.PriceFormatter
import com.example.data.model.Listing
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ListingPlaceholderArt
import com.example.ui.components.QuickContactListingDialog
import com.example.ui.components.SecondaryPageScaffold
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.MarketZedOrange

@Composable
fun ComparisonScreen(
    state: ComparisonUiState,
    dataSaverEnabled: Boolean,
    shareSummary: String,
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    onOpenSeller: (Listing) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onToggleShowDifferencesOnly: (Boolean) -> Unit,
    onBrowseProducts: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var contactListing by remember { mutableStateOf<Listing?>(null) }

    SecondaryPageScaffold(
        title = "Compare Products (${state.count}/3)",
        onBack = onBack,
        backButtonTestTag = "compare_screen_back_button",
        actions = {
            if (state.items.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "MarketZed Product Comparison")
                            putExtra(Intent.EXTRA_TEXT, shareSummary)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share comparison"))
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("compare_share_button")
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_share_2),
                        contentDescription = "Share comparison"
                    )
                }
            }
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    iconRes = LucideR.drawable.lucide_ic_arrow_left_right,
                    title = "No listings selected to compare",
                    message = "Tap Compare on any product, rental property, swap, or free listing to inspect up to 3 items side by side.",
                    actionLabel = "Explore Marketplace",
                    onAction = onBrowseProducts
                )
            }
        } else {
            val horizontalScroll = rememberScrollState()
            val labelColWidth = 112.dp
            val itemColWidth = 176.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("comparison_matrix_screen"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !state.showDifferencesOnly,
                            onClick = { onToggleShowDifferencesOnly(false) },
                            label = { Text("Show All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MarketZedOrange,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("compare_filter_all")
                        )
                        FilterChip(
                            selected = state.showDifferencesOnly,
                            onClick = { onToggleShowDifferencesOnly(true) },
                            label = { Text("Show Differences (${state.differingAttributes.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MarketZedOrange,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("compare_filter_differences")
                        )
                    }

                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("compare_clear_all_button")
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }

                // Horizontally scrollable side-by-side matrix
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .horizontalScroll(horizontalScroll)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header row: Empty attribute label + Product Cards
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.width(labelColWidth).padding(top = 8.dp)) {
                                Text(
                                    text = "Listing",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            state.items.forEach { item ->
                                val listing = item.listing
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.width(itemColWidth)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable(enabled = listing != null && !item.isUnavailable) {
                                                    listing?.let { onOpenListing(it.id) }
                                                }
                                        ) {
                                            val firstImg = listing?.images?.firstOrNull()
                                            val url = CloudinaryUrlHelper.optimizeForFeed(firstImg?.url, firstImg?.thumbUrl, dataSaverEnabled)
                                            if (listing != null && !listing.isWanted && !url.isNullOrBlank()) {
                                                SubcomposeAsyncImage(
                                                    model = url,
                                                    contentDescription = listing.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                    error = { ListingPlaceholderArt(listing.categoryId) }
                                                )
                                            } else {
                                                ListingPlaceholderArt(listing?.categoryId ?: "marketplace")
                                            }

                                            IconButton(
                                                onClick = { onRemoveItem(item.listingId) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(36.dp)
                                                    .testTag("compare_remove_${item.listingId}")
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            painter = painterResource(LucideR.drawable.lucide_ic_x),
                                                            contentDescription = "Remove item",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (item.isUnavailable || listing == null) {
                                            StatusBadgePill(
                                                text = item.unavailableReason ?: "Unavailable",
                                                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                                                textColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = listing?.title ?: "Removed listing",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = listing.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.clickable { onOpenListing(listing.id) }
                                            )
                                            Text(
                                                text = PriceFormatter.formatListingPrice(listing.price, listing.priceMode, listing.categoryId),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MarketZedOrange
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = { contactListing = listing },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                                ) {
                                                    Text("Contact", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = { onOpenListing(listing.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                                ) {
                                                    Text("View", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        if (state.specRows.isEmpty()) {
                            Text(
                                text = "All compared attributes match across these items.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            state.specRows.forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (row.hasDifference) MarketZedOrange.copy(alpha = 0.08f)
                                            else Color.Transparent
                                        )
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = row.attribute,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (row.hasDifference) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (row.hasDifference) MarketZedOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(labelColWidth)
                                    )
                                    row.values.forEach { value ->
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (row.hasDifference && value != "Not provided") FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (value == "Not provided" || value == "Unavailable") {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.width(itemColWidth)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    contactListing?.let { target ->
        QuickContactListingDialog(
            listing = target,
            onDismiss = { contactListing = null },
            onViewDetails = { onOpenListing(target.id) },
            onOpenSellerProfile = { onOpenSeller(target) }
        )
    }
}
