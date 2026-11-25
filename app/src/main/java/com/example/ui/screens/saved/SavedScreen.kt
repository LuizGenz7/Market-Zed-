package com.example.ui.screens.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.data.model.Listing
import com.example.ui.components.CategoryAwareFullWidthListingCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.QuickContactListingDialog
import com.example.ui.theme.MarketZedOrange

@Composable
fun SavedScreen(
    state: SavedUiState,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearRecentlyViewed: () -> Unit,
    onExploreClick: () -> Unit,
    comparedIds: Set<String> = emptySet(),
    onToggleCompare: ((Listing) -> Unit)? = null,
    onOpenSeller: (Listing) -> Unit = {},
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var contactListing by remember { mutableStateOf<Listing?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            item {
                Text(
                    text = "Saved & History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("saved_tab_favorites")
                ) {
                    Text("Saved (${state.favorites.size})")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("saved_tab_recent")
                ) {
                    Text("Recent (${state.recentlyViewed.size})")
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Saved favorites and recently viewed snapshots are stored locally on this device for offline access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (selectedTab == 0) {
            if (state.favorites.isEmpty()) {
                item {
                    EmptyStateView(
                        iconRes = LucideR.drawable.lucide_ic_bookmark,
                        title = "No saved listings yet",
                        message = "Tap the heart on any listing to keep it handy here, even when you are offline.",
                        actionLabel = "Explore Marketplace",
                        onAction = onExploreClick
                    )
                }
            } else {
                items(state.favorites, key = { "saved_fav_${it.id}" }) { item ->
                    CategoryAwareFullWidthListingCard(
                        listing = item,
                        isFavorite = item.id in state.favoriteIds,
                        isCompared = item.id in comparedIds,
                        dataSaverEnabled = state.settings.dataSaverEnabled,
                        onClick = { onOpenListing(item.id) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onToggleCompare = onToggleCompare?.let { cb -> { cb(item) } },
                        onContactClick = { contactListing = item },
                        onReportClick = { onOpenListing(item.id) },
                        onSellerClick = { onOpenSeller(item) }
                    )
                }
            }
        } else {
            if (state.recentlyViewed.isEmpty()) {
                item {
                    EmptyStateView(
                        iconRes = LucideR.drawable.lucide_ic_history,
                        title = "No recently viewed items",
                        message = "Listings you open will appear here so you can revisit them quickly.",
                        actionLabel = "Browse Listings",
                        onAction = onExploreClick
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onClearRecentlyViewed,
                            modifier = Modifier.testTag("clear_recent_history_button")
                        ) {
                            Text("Clear history", color = MarketZedOrange)
                        }
                    }
                }
                items(state.recentlyViewed, key = { "saved_rec_${it.id}" }) { item ->
                    CategoryAwareFullWidthListingCard(
                        listing = item,
                        isFavorite = item.id in state.favoriteIds,
                        isCompared = item.id in comparedIds,
                        dataSaverEnabled = state.settings.dataSaverEnabled,
                        onClick = { onOpenListing(item.id) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onToggleCompare = onToggleCompare?.let { cb -> { cb(item) } },
                        onContactClick = { contactListing = item },
                        onReportClick = { onOpenListing(item.id) },
                        onSellerClick = { onOpenSeller(item) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
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
