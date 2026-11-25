package com.example.ui.screens.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.CloudinaryUrlHelper
import com.example.core.util.PriceFormatter
import com.example.ui.components.ListingPlaceholderArt
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.MarketZedOrange

/**
 * Compact persistent bar rendered cleanly above the BottomBar when 1..3 items are queued for comparison.
 */
@Composable
fun ComparisonFloatingBar(
    state: ComparisonUiState,
    onOpenQuickSheet: () -> Unit,
    onOpenFullComparison: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.count == 0) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("comparison_floating_bar"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, MarketZedOrange),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenQuickSheet)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MarketZedOrange
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_arrow_left_right),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp).size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "Compare ${state.count}/3",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val summary = state.items.mapNotNull { it.listing?.title }.joinToString(" · ")
                    if (summary.isNotBlank()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onOpenQuickSheet,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("compare_bar_quick_button")
                ) {
                    Text("Quick", color = MarketZedOrange, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onOpenFullComparison,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("compare_bar_open_button")
                ) {
                    Text("Open", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.size(48.dp).testTag("compare_bar_clear_button")
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_x),
                        contentDescription = "Clear comparison",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickComparisonSheet(
    state: ComparisonUiState,
    dataSaverEnabled: Boolean,
    onDismiss: () -> Unit,
    onOpenListing: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddProductClick: () -> Unit,
    onExpandComparison: () -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("quick_comparison_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Comparison (${state.count}/3)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Compare up to 3 listings side by side.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onClearAll) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }

            state.items.forEach { item ->
                val listing = item.listing
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (item.isUnavailable || listing == null) {
                                StatusBadgePill(
                                    text = item.unavailableReason ?: "Unavailable",
                                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                                    textColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = listing?.title ?: "Listing (${item.listingId})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = listing.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = PriceFormatter.formatListingPrice(listing.price, listing.priceMode, listing.categoryId),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MarketZedOrange
                                )
                                Text(
                                    text = buildString {
                                        listing.categoryDetails?.condition?.takeIf { it.isNotBlank() }?.let { append("$it · ") }
                                        append(listing.location?.displayTown ?: "Zambia")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(
                                onClick = { onRemoveItem(item.listingId) },
                                modifier = Modifier.size(48.dp).testTag("quick_compare_remove_${item.listingId}")
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_x),
                                    contentDescription = "Remove from comparison",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (listing != null && !item.isUnavailable) {
                                TextButton(onClick = {
                                    onDismiss()
                                    onOpenListing(listing.id)
                                }) {
                                    Text("View", color = MarketZedOrange)
                                }
                            }
                        }
                    }
                }
            }

            if (state.count < 3) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onAddProductClick()
                    },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) {
                    Icon(painter = painterResource(LucideR.drawable.lucide_ic_plus), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add product to compare (${3 - state.count} slot${if (3 - state.count == 1) "" else "s"} left)")
                }
            }

            Button(
                onClick = {
                    onDismiss()
                    onExpandComparison()
                },
                enabled = state.items.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("quick_compare_expand_button")
            ) {
                Text("Expand Full Side-by-Side Comparison")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
