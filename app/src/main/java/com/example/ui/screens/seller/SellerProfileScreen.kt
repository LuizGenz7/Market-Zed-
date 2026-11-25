package com.example.ui.screens.seller

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.PhoneValidator
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.ui.components.CategoryAwareFullWidthListingCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FullWidthListingSkeletonSection
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.QuickContactListingDialog
import com.example.ui.components.SecondaryPageScaffold
import com.example.ui.components.StatusBadgePill
import com.example.ui.components.UserAvatarBadge
import com.example.ui.components.VerticalContactActionsColumn
import com.example.ui.components.WrapRow
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald

@Composable
fun SellerProfileScreen(
    state: SellerProfileUiState,
    comparedIds: Set<String>,
    onBack: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleCompare: (Listing) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val snackbar = LocalTopSnackbarController.current
    var contactListing by remember { mutableStateOf<Listing?>(null) }

    SecondaryPageScaffold(
        title = state.profile?.displayName ?: "Seller Storefront",
        onBack = onBack,
        backButtonTestTag = "seller_profile_back_button"
    ) { padding ->
        val profile = state.profile
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    FullWidthListingSkeletonSection(count = 3)
                }
            }
            profile == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        iconRes = LucideR.drawable.lucide_ic_user,
                        title = "Seller storefront unavailable",
                        message = "No active public listings were found for this seller yet.",
                        actionLabel = "Go back",
                        onAction = onBack
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("seller_profile_screen"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    UserAvatarBadge(
                                        photoUrl = profile.photoUrl,
                                        displayName = profile.displayName,
                                        size = 56.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = profile.displayName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        WrapRow(
                                            horizontalSpacing = 6.dp,
                                            verticalSpacing = 4.dp
                                        ) {
                                            StatusBadgePill(
                                                text = if (profile.accountType.equals("business", ignoreCase = true)) {
                                                    "Business Seller"
                                                } else {
                                                    "Personal Account"
                                                },
                                                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                                textColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            if (profile.verified) {
                                                StatusBadgePill(
                                                    text = "Verified",
                                                    backgroundColor = VerifiedEmerald,
                                                    textColor = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                                    Text(
                                        text = bio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                WrapRow(
                                    horizontalSpacing = 12.dp,
                                    verticalSpacing = 4.dp
                                ) {
                                    profile.primaryLocation?.takeIf { it.isNotBlank() }?.let { loc ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(
                                                painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(loc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    profile.memberSince?.takeIf { it.isNotBlank() }?.let { since ->
                                        Text(
                                            text = "Member since $since",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${profile.listings.size} active ${if (profile.listings.size == 1) "listing" else "listings"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MarketZedOrange
                                    )
                                }

                                val prefs = profile.contactPreferences
                                if (prefs != null && prefs.hasAny) {
                                    VerticalContactActionsColumn(
                                        contactPreferences = prefs,
                                        listingTitle = profile.displayName,
                                        categoryId = profile.listings.firstOrNull()?.categoryId ?: "marketplace",
                                        sellerAccountType = profile.accountType
                                    )
                                }
                            }
                        }
                    }

                    if (state.availableCategoryIds.size > 1) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = state.selectedCategoryId == null,
                                        onClick = { onSelectCategory(null) },
                                        label = { Text("All (${profile.listings.size})") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MarketZedOrange,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                                items(state.availableCategoryIds) { catId ->
                                    FilterChip(
                                        selected = state.selectedCategoryId == catId,
                                        onClick = { onSelectCategory(catId) },
                                        label = { Text(MarketZedCatalogue.categoryLabel(catId)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MarketZedOrange,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (state.filteredListings.isEmpty()) {
                        item {
                            EmptyStateView(
                                iconRes = LucideR.drawable.lucide_ic_package,
                                title = "No active listings",
                                message = "This seller currently has no active public listings in this category."
                            )
                        }
                    } else {
                        items(state.filteredListings, key = { "seller_item_${it.id}" }) { listing ->
                            CategoryAwareFullWidthListingCard(
                                listing = listing,
                                isFavorite = listing.id in state.favoriteIds,
                                isCompared = listing.id in comparedIds,
                                dataSaverEnabled = state.settings.dataSaverEnabled,
                                onClick = { onOpenListing(listing.id) },
                                onToggleFavorite = { onToggleFavorite(listing.id) },
                                onToggleCompare = if (listing.isComparableCategory) {
                                    { onToggleCompare(listing) }
                                } else null,
                                onContactClick = { contactListing = listing }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    contactListing?.let { target ->
        QuickContactListingDialog(
            listing = target,
            onDismiss = { contactListing = null },
            onViewDetails = { onOpenListing(target.id) }
        )
    }
}
