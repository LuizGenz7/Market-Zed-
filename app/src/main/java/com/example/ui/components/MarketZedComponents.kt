package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.annotation.DrawableRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.CloudinaryUrlHelper
import com.example.core.util.DateUtils
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldContainerLight
import com.example.ui.theme.WantedBlue
import com.example.ui.theme.WantedBlueContainerLight

enum class TopLevelTab(val route: String, val label: String) {
    HOME("home", "Home"),
    EXPLORE("explore", "Explore"),
    POST("post", "Post"),
    WANTED("wanted", "Wanted"),
    ACCOUNT("account", "Account")
}

@Composable
fun MarketZedBottomBar(
    currentRoute: String?,
    onNavigate: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    tab = TopLevelTab.HOME,
                    selected = currentRoute == TopLevelTab.HOME.route,
                    iconRes = LucideR.drawable.lucide_ic_house,
                    onClick = { onNavigate(TopLevelTab.HOME) }
                )
                BottomNavItem(
                    tab = TopLevelTab.EXPLORE,
                    selected = currentRoute == TopLevelTab.EXPLORE.route,
                    iconRes = LucideR.drawable.lucide_ic_compass,
                    onClick = { onNavigate(TopLevelTab.EXPLORE) }
                )
                // Prominent central orange + Post action
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_post")
                        .semantics { contentDescription = "Post a new listing" }
                        .clickable { onNavigate(TopLevelTab.POST) }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MarketZedOrange,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_plus),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = TopLevelTab.POST.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentRoute == TopLevelTab.POST.route) MarketZedOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                BottomNavItem(
                    tab = TopLevelTab.WANTED,
                    selected = currentRoute == TopLevelTab.WANTED.route,
                    iconRes = LucideR.drawable.lucide_ic_handshake,
                    onClick = { onNavigate(TopLevelTab.WANTED) }
                )
                BottomNavItem(
                    tab = TopLevelTab.ACCOUNT,
                    selected = currentRoute == TopLevelTab.ACCOUNT.route,
                    iconRes = LucideR.drawable.lucide_ic_user,
                    onClick = { onNavigate(TopLevelTab.ACCOUNT) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: TopLevelTab,
    selected: Boolean,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    val tint = if (selected) MarketZedOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .testTag("nav_${tab.route}")
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun OfflineStaleBanner(
    isOffline: Boolean,
    isStale: Boolean,
    lastSyncedAt: Long?,
    onRetrySync: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!isOffline && !isStale) return
    val bgColor = if (isOffline) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val contentColor = if (isOffline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_stale_banner"),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_wifi_off),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val syncText = lastSyncedAt?.takeIf { it > 0L }?.let {
                    "Last synced ${DateUtils.formatRelativeTime(it)}"
                }
                val label = when {
                    isOffline && syncText != null -> "Offline · Showing cached snapshot ($syncText)"
                    isOffline -> "Offline · Cached data only"
                    syncText != null -> "Showing cached feed · $syncText"
                    else -> "Cached feed · Pull to refresh"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onRetrySync != null) {
                IconButton(
                    onClick = onRetrySync,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_refresh_cw),
                        contentDescription = "Sync now",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ListingCard(
    listing: Listing,
    isFavorite: Boolean,
    dataSaverEnabled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isCompared: Boolean = false,
    onToggleCompare: (() -> Unit)? = null,
    onSellerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firstImg = listing.images.firstOrNull()
    val optimizedImageUrl = CloudinaryUrlHelper.optimizeForFeed(
        url = firstImg?.url,
        thumbUrl = firstImg?.thumbUrl,
        dataSaverEnabled = dataSaverEnabled
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("listing_card_${listing.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!listing.isWanted && !optimizedImageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = optimizedImageUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(rememberShimmerBrush())
                            )
                        },
                        error = {
                            ListingPlaceholderArt(categoryId = listing.categoryId)
                        }
                    )
                } else {
                    ListingPlaceholderArt(categoryId = listing.categoryId)
                }

                // Top-left badges
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (listing.isPromoted) {
                        StatusBadgePill(
                            text = "Sponsored",
                            backgroundColor = MarketZedOrange,
                            textColor = Color.White
                        )
                    }
                    if (listing.categoryId.equals("property", ignoreCase = true)) {
                        StatusBadgePill(
                            text = "For Rent",
                            backgroundColor = VerifiedEmerald,
                            textColor = Color.White
                        )
                    }
                    listing.priceDropPercent?.let { pct ->
                        StatusBadgePill(
                            text = "Save $pct%",
                            backgroundColor = VerifiedEmerald,
                            textColor = Color.White
                        )
                    }
                    if (listing.isWanted) {
                        StatusBadgePill(
                            text = "WANTED",
                            backgroundColor = WantedBlue,
                            textColor = Color.White
                        )
                    }
                }

                // Top-right favorite toggle (48dp touch target, compact visual pill)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .testTag("favorite_toggle_${listing.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isFavorite) MarketZedOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_heart),
                                contentDescription = if (isFavorite) "Remove ${listing.title} from saved" else "Save ${listing.title}",
                                tint = if (isFavorite) MarketZedOrange else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = MarketZedCatalogue.categoryLabel(listing.categoryId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MarketZedOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (listing.sellerVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_badge_check),
                                contentDescription = "Verified account",
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = VerifiedEmerald
                            )
                        }
                    }
                }

                Text(
                    text = listing.title.ifBlank { "Untitled listing" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = PriceFormatter.formatListingPrice(
                        price = listing.price,
                        priceMode = listing.priceMode,
                        categoryId = listing.categoryId
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                listing.sellerName?.takeIf { it.isNotBlank() }?.let { seller ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (onSellerClick != null) Modifier.clickable(onClick = onSellerClick) else Modifier)
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_user),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = seller,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onToggleCompare != null && listing.isComparableCategory) {
                    ListingCompareToggleButton(
                        listing = listing,
                        isCompared = isCompared,
                        onToggle = onToggleCompare
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = listing.location?.displayTown ?: "Location not specified",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateUtils.formatRelativeTime(listing.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun FullWidthSecondaryListingCard(
    listing: Listing,
    isFavorite: Boolean,
    dataSaverEnabled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isCompared: Boolean = false,
    onToggleCompare: (() -> Unit)? = null,
    onContactClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onSellerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CategoryAwareFullWidthListingCard(
        listing = listing,
        isFavorite = isFavorite,
        isCompared = isCompared,
        dataSaverEnabled = dataSaverEnabled,
        onClick = onClick,
        onToggleFavorite = onToggleFavorite,
        onToggleCompare = onToggleCompare,
        onContactClick = onContactClick,
        onReportClick = onReportClick,
        onSellerClick = onSellerClick,
        modifier = modifier
    )
}

@Composable
fun UserAvatarBadge(
    photoUrl: String?,
    displayName: String?,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(displayName) {
        displayName
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            ?.take(2)
            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
            ?.joinToString("")
            .orEmpty()
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MarketZedOrange.copy(alpha = 0.3f)),
        modifier = modifier.size(size)
    ) {
        if (!photoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = displayName ?: "Account avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                error = {
                    UserAvatarFallbackContent(initials = initials, size = size)
                }
            )
        } else {
            UserAvatarFallbackContent(initials = initials, size = size)
        }
    }
}

@Composable
private fun UserAvatarFallbackContent(
    initials: String,
    size: Dp
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (initials.isNotBlank() && initials != "MU") {
            Text(
                text = initials,
                style = if (size >= 44.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MarketZedOrange
            )
        } else {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_user),
                contentDescription = "Account avatar",
                tint = MarketZedOrange,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

@Composable
fun SubtleRefreshBar(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180)),
        modifier = modifier
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .testTag("subtle_refresh_bar"),
            color = MarketZedOrange,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

data class ActiveFilterChipItem(
    val id: String,
    val label: String,
    val onClear: () -> Unit
)

@Composable
fun ActiveFilterChipsBar(
    items: List<ActiveFilterChipItem>,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items, key = { it.id }) { chip ->
            FilterChip(
                selected = true,
                onClick = chip.onClear,
                label = {
                    Text(
                        text = chip.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_x),
                        contentDescription = "Clear ${chip.label}",
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
        if (items.size > 1) {
            item(key = "clear_all_active") {
                FilterChip(
                    selected = false,
                    onClick = onClearAll,
                    label = { Text("Reset all") }
                )
            }
        }
    }
}

@Composable
fun ListingPlaceholderArt(categoryId: String, modifier: Modifier = Modifier) {
    val isWanted = categoryId.equals("wanted", ignoreCase = true)
    val isJobs = categoryId.equals("jobs", ignoreCase = true)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                when {
                    isWanted -> WantedBlueContainerLight.copy(alpha = 0.45f)
                    isJobs -> VerifiedEmeraldContainerLight.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(if (isWanted) LucideR.drawable.lucide_ic_handshake else LucideR.drawable.lucide_ic_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MarketZedCatalogue.categoryLabel(categoryId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatusBadgePill(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyStateView(
    @DrawableRes iconRes: Int,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MarketZedOrange.copy(alpha = 0.12f),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MarketZedOrange,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ErrorStateView(
    title: String,
    message: String,
    retryLabel: String = "Try again",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(retryLabel)
            }
        }
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface
    return Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.72f),
            highlightColor.copy(alpha = 0.94f),
            baseColor.copy(alpha = 0.72f)
        ),
        start = Offset(translateAnim - 320f, 0f),
        end = Offset(translateAnim, 260f)
    )
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    brush: Brush,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun ListingCardSkeleton(
    brush: Brush = rememberShimmerBrush(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f),
                brush = brush,
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(10.dp),
                    brush = brush
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(14.dp),
                    brush = brush
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .height(14.dp),
                    brush = brush
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp),
                    brush = brush
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.52f)
                            .height(10.dp),
                        brush = brush
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .width(34.dp)
                            .height(10.dp),
                        brush = brush
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingSkeletonSection(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(2) {
            ListingCardSkeleton(
                brush = brush,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ListRowSkeletonSection(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.58f)
                                .height(14.dp),
                            brush = brush
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .width(64.dp)
                                .height(18.dp),
                            brush = brush,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .height(12.dp),
                        brush = brush
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(10.dp),
                        brush = brush
                    )
                }
            }
        }
    }
}

@Composable
fun FullWidthListingSkeletonSection(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkeletonBlock(
                        modifier = Modifier.size(88.dp),
                        brush = brush,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.45f).height(11.dp), brush = brush)
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp), brush = brush)
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.38f).height(14.dp), brush = brush)
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.65f).height(10.dp), brush = brush)
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionSkeletonSection(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                SkeletonBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp),
                    brush = brush,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        ListRowSkeletonSection(count = 2)
    }
}

@Composable
fun ListingDetailSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            brush = brush,
            shape = RoundedCornerShape(0.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(modifier = Modifier.width(84.dp).height(24.dp), brush = brush)
                SkeletonBlock(modifier = Modifier.width(72.dp).height(24.dp), brush = brush)
            }
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.85f).height(24.dp), brush = brush)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.42f).height(26.dp), brush = brush)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.62f).height(14.dp), brush = brush)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.72f).height(14.dp), brush = brush)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(14.dp), brush = brush)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(12.dp), brush = brush)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp), brush = brush)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp), brush = brush)
                }
            }
        }
    }
}

/**
 * Standardized secondary-page layout across MarketZed.
 * Eliminates duplicate window insets and double-padding between status bar and content,
 * standardizes the app bar alignment and back button, and provides consistent bottom spacing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    backButtonTestTag: String = "secondary_back_button",
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(backButtonTestTag)
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_arrow_left),
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = actions,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        content = content
    )
}

/**
 * Clean, refined OutlinedTextField with 1.5dp focus border and 1dp unfocused border,
 * preserving MarketZed Orange accent and rounded corners.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarketZedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MarketZedOrange,
        focusedLabelColor = MarketZedOrange,
        cursorColor = MarketZedOrange,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    ),
    contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val textColor = if (textStyle.color != Color.Unspecified) {
        textStyle.color
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val mergedTextStyle = textStyle.copy(color = textColor)

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val extraBottomPx = with(density) { 28.dp.toPx() }

    LaunchedEffect(isFocused, imeBottom) {
        if (isFocused && imeBottom > 0 && fieldSize != IntSize.Zero) {
            delay(150)
            bringIntoViewRequester.bringIntoView(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = fieldSize.width.toFloat(),
                    bottom = fieldSize.height.toFloat() + extraBottomPx
                )
            )
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onSizeChanged { fieldSize = it }
            .onFocusChanged { isFocused = it.isFocused },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(if (isError) MaterialTheme.colorScheme.error else MarketZedOrange),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox = @Composable { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                visualTransformation = visualTransformation,
                innerTextField = innerTextField,
                placeholder = placeholder,
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                prefix = prefix,
                suffix = suffix,
                supportingText = supportingText,
                singleLine = singleLine,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = contentPadding,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape,
                        focusedBorderThickness = 1.5.dp,
                        unfocusedBorderThickness = 1.dp
                    )
                }
            )
        }
    )
}
