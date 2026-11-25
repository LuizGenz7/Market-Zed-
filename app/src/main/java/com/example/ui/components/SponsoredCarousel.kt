package com.example.ui.components

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.model.SponsoredPlacementBanner
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.MarketZedOrangeDark
import com.example.ui.theme.VerifiedEmerald
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Native Jetpack Compose [1] [3] [1] flex-proportion sponsored showcase carousel.
 * Center focused card occupies ~60% of available width (proportion 3) while flanking
 * peeking cards occupy ~20% each (proportion 1) on handhelds and scale cleanly on tablets.
 */
@Composable
fun FlexibleSponsoredCarousel(
    listings: List<Listing>,
    favoriteIds: Set<String>,
    dataSaverEnabled: Boolean,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (listings.isEmpty()) return

    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val activeIndex by remember(listState, listings.size) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - viewportCenter)
                }?.index?.coerceIn(0, listings.lastIndex) ?: 0
            }
        }
    }

    // Gentle auto-advance only when reduced-motion is off, multiple cards exist, and user isn't dragging
    LaunchedEffect(listings.size, reducedMotion) {
        if (!reducedMotion && listings.size > 1) {
            while (true) {
                delay(5500L)
                if (!listState.isScrollInProgress) {
                    val next = (activeIndex + 1) % listings.size
                    listState.animateScrollToItem(next)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsored_131_carousel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth * 0.62f).coerceAtMost(380.dp)
            val sidePeekPadding = ((maxWidth - cardWidth) / 2f).coerceAtLeast(16.dp)

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = sidePeekPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(listings, key = { _, item -> "carousel_${item.id}" }) { index, listing ->
                    val isCenter = index == activeIndex
                    val firstImg = listing.images.firstOrNull()
                    val imageUrl = CloudinaryUrlHelper.optimizeForFeed(
                        url = firstImg?.url,
                        thumbUrl = firstImg?.thumbUrl,
                        dataSaverEnabled = dataSaverEnabled
                    )
                    val isFav = listing.id in favoriteIds

                    Card(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(216.dp)
                            .graphicsLayer {
                                val scale = if (isCenter || reducedMotion) 1f else 0.93f
                                scaleX = scale
                                scaleY = scale
                                alpha = if (isCenter || reducedMotion) 1f else 0.84f
                            }
                            .testTag("sponsored_carousel_card_${listing.id}")
                            .clickable { onOpenListing(listing.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            width = if (isCenter) 1.5.dp else 1.dp,
                            color = if (isCenter) MarketZedOrange.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isCenter) 3.dp else 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!imageUrl.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = imageUrl,
                                    contentDescription = listing.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize().background(rememberShimmerBrush()))
                                    },
                                    error = {
                                        ListingPlaceholderArt(categoryId = listing.categoryId)
                                    }
                                )
                            } else {
                                ListingPlaceholderArt(categoryId = listing.categoryId)
                            }

                            // Bottom dark scrim for legible text overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(114.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                                        )
                                    )
                            )

                            // Top badges
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusBadgePill(
                                    text = "Sponsored",
                                    backgroundColor = MarketZedOrange,
                                    textColor = Color.White
                                )
                                listing.priceDropPercent?.let { pct ->
                                    StatusBadgePill(
                                        text = "Save $pct%",
                                        backgroundColor = VerifiedEmerald,
                                        textColor = Color.White
                                    )
                                }
                            }

                            // Favorite button
                            IconButton(
                                onClick = { onToggleFavorite(listing.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .testTag("carousel_fav_${listing.id}")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isFav) MarketZedOrange else Color.Black.copy(alpha = 0.45f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(LucideR.drawable.lucide_ic_heart),
                                            contentDescription = if (isFav) "Remove from saved" else "Save listing",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            // Overlay metadata
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = MarketZedCatalogue.categoryLabel(listing.categoryId),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                                Text(
                                    text = listing.title.ifBlank { "Featured Listing" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = PriceFormatter.formatListingPrice(
                                            price = listing.price,
                                            priceMode = listing.priceMode,
                                            categoryId = listing.categoryId
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MarketZedOrange
                                    )
                                    Text(
                                        text = listing.location?.displayTown ?: "Zambia",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (listings.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listings.forEachIndexed { idx, _ ->
                    val selected = idx == activeIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (selected) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MarketZedOrange
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun SponsoredHeroBannerCard(
    banner: SponsoredPlacementBanner,
    dataSaverEnabled: Boolean,
    onOpenListing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listing = banner.listing
    val firstImg = listing.images.firstOrNull()
    val imageUrl = CloudinaryUrlHelper.optimizeForFeed(
        url = firstImg?.url,
        thumbUrl = firstImg?.thumbUrl,
        dataSaverEnabled = dataSaverEnabled
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsored_hero_banner_${listing.id}")
            .clickable { onOpenListing(listing.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MarketZedOrange.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MarketZedOrange.copy(alpha = 0.12f),
                            MarketZedOrangeDark.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = { ListingPlaceholderArt(categoryId = listing.categoryId) }
                    )
                } else {
                    ListingPlaceholderArt(categoryId = listing.categoryId)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadgePill("Sponsored", MarketZedOrange, Color.White)
                    listing.priceDropPercent?.let { pct ->
                        StatusBadgePill("Save $pct%", VerifiedEmerald, Color.White)
                    }
                }
                Text(
                    text = banner.headline.ifBlank { listing.title },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${PriceFormatter.formatListingPrice(listing.price, listing.priceMode, listing.categoryId)} · ${banner.subheadline}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = { onOpenListing(listing.id) },
                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("View", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CompactSponsoredStrip(
    title: String,
    listings: List<Listing>,
    favoriteIds: Set<String>,
    dataSaverEnabled: Boolean,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (listings.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(listings, key = { _, l -> "strip_${l.id}" }) { _, listing ->
                Box(modifier = Modifier.width(184.dp)) {
                    ListingCard(
                        listing = listing,
                        isFavorite = listing.id in favoriteIds,
                        dataSaverEnabled = dataSaverEnabled,
                        onClick = { onOpenListing(listing.id) },
                        onToggleFavorite = { onToggleFavorite(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySponsoredBanner(
    categoryLabel: String,
    listings: List<Listing>,
    dataSaverEnabled: Boolean,
    onOpenListing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lead = listings.firstOrNull() ?: return
    SponsoredHeroBannerCard(
        banner = SponsoredPlacementBanner(
            listing = lead,
            format = com.example.data.model.SponsoredFormat.CATEGORY_BANNER,
            headline = lead.title,
            subheadline = "Featured in $categoryLabel · ${lead.location?.displayTown ?: "Zambia"}"
        ),
        dataSaverEnabled = dataSaverEnabled,
        onOpenListing = onOpenListing,
        modifier = modifier
    )
}
