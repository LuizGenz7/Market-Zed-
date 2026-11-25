package com.example.ui.screens.wanted

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.data.model.Listing
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.OfflineStaleBanner
import com.example.ui.components.QuickContactListingDialog
import com.example.ui.components.SubtleRefreshBar
import com.example.ui.components.WantedTextOnlyCard
import com.example.ui.components.WantedTextOnlySkeletonSection
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.WantedBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WantedScreen(
    state: WantedUiState,
    onQueryChanged: (String) -> Unit,
    onSelectProvince: (String?) -> Unit,
    onSelectSort: (String) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPostWanted: () -> Unit,
    onClearError: () -> Unit,
    onOpenSeller: (Listing) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var provinceMenuExpanded by remember { mutableStateOf(false) }
    var contactListing by remember { mutableStateOf<Listing?>(null) }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.errorMessage) {
        val err = state.errorMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showWarning(err, actionLabel = "Retry", onAction = onRefresh)
        onClearError()
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubtleRefreshBar(visible = state.isRefreshing && state.listings.isNotEmpty())

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wanted Requests",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Post a buyer request — your first Wanted post is free.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onPostWanted,
                            colors = ButtonDefaults.buttonColors(containerColor = WantedBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("wanted_post_free_button")
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_handshake),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Post Wanted", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Compact Search + Province Filter Row
                item {
                    MarketZedOutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChanged,
                        placeholder = {
                            Text(
                                text = "Search wanted requests...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_search),
                                contentDescription = null,
                                tint = WantedBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChanged("") }) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_x),
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onRefresh() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wanted_search_input")
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            ExposedDropdownMenuBox(
                                expanded = provinceMenuExpanded,
                                onExpandedChange = { provinceMenuExpanded = it }
                            ) {
                                FilterChip(
                                    selected = state.selectedProvince != null,
                                    onClick = { provinceMenuExpanded = true },
                                    label = {
                                        Text(
                                            text = state.selectedProvince ?: "All Provinces",
                                            maxLines = 1
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceMenuExpanded)
                                    },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = provinceMenuExpanded,
                                    onDismissRequest = { provinceMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Provinces") },
                                        onClick = {
                                            onSelectProvince(null)
                                            provinceMenuExpanded = false
                                        }
                                    )
                                    state.provinces.forEach { prov ->
                                        DropdownMenuItem(
                                            text = { Text(prov.province) },
                                            onClick = {
                                                onSelectProvince(prov.province)
                                                provinceMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            FilterChip(
                                selected = state.sort == "newest",
                                onClick = { onSelectSort("newest") },
                                label = { Text("Newest") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MarketZedOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.sort == "oldest",
                                onClick = { onSelectSort("oldest") },
                                label = { Text("Oldest") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MarketZedOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        if (state.query.isNotBlank() || state.selectedProvince != null || state.sort != "newest") {
                            item {
                                TextButton(onClick = onClearFilters) {
                                    Text("Clear", color = MarketZedOrange)
                                }
                            }
                        }
                    }
                }

                if (state.isOffline || (!state.isRefreshing && state.isStale && state.lastSyncedAt != null)) {
                    item {
                        OfflineStaleBanner(
                            isOffline = state.isOffline,
                            isStale = state.isStale,
                            lastSyncedAt = state.lastSyncedAt,
                            onRetrySync = onRefresh
                        )
                    }
                }

                if (state.isLoading) {
                    item { WantedTextOnlySkeletonSection(count = 4) }
                } else if (state.listings.isEmpty()) {
                    item {
                        EmptyStateView(
                            iconRes = LucideR.drawable.lucide_ic_handshake,
                            title = if (state.isOffline) "No cached Wanted requests" else "No Wanted requests found",
                            message = "Need an item, rental room, or service in Zambia? Your first Wanted post is FREE, and verified accounts can publish unlimited requests.",
                            actionLabel = if (state.query.isNotBlank() || state.selectedProvince != null) "Clear Filters" else "Post Wanted Request",
                            onAction = if (state.query.isNotBlank() || state.selectedProvince != null) onClearFilters else onPostWanted
                        )
                    }
                } else {
                    items(state.listings, key = { it.id }) { listing ->
                        WantedTextOnlyCard(
                            listing = listing,
                            isFavorite = listing.id in state.favoriteIds,
                            onClick = { onOpenListing(listing.id) },
                            onToggleFavorite = { onToggleFavorite(listing.id) },
                            onContactClick = { contactListing = listing },
                            onReportClick = { onOpenListing(listing.id) },
                            onSellerClick = { onOpenSeller(listing) }
                        )
                    }

                    if (state.isLoadingMore) {
                        item { WantedTextOnlySkeletonSection(count = 2) }
                    } else if (state.nextCursor != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = onLoadNextPage,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Load more Wanted requests")
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
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
