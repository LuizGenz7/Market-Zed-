package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.CloudinaryUrlHelper
import com.example.core.util.DateUtils
import com.example.core.util.PhoneValidator
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WantedBlue

/**
 * Accessible comparison selection pill/icon (minimum 48dp touch target).
 * Visible only for comparable marketplace items (Products, Rental properties, Swaps, Free).
 */
@Composable
fun ListingCompareToggleButton(
    listing: Listing,
    isCompared: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!listing.isComparableCategory) return
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isCompared) MarketZedOrange else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isCompared) MarketZedOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .testTag("compare_toggle_${listing.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isCompared) LucideR.drawable.lucide_ic_circle_check
                    else LucideR.drawable.lucide_ic_arrow_left_right
                ),
                contentDescription = if (isCompared) "Remove ${listing.title} from comparison" else "Compare ${listing.title}",
                tint = if (isCompared) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isCompared) "Compared" else "Compare",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompared) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WrapRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hSpacePx = horizontalSpacing.roundToPx()
        val vSpacePx = verticalSpacing.roundToPx()
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(childConstraints)
            val neededWidth = if (currentRow.isEmpty()) placeable.width else currentRowWidth + hSpacePx + placeable.width
            if (currentRow.isNotEmpty() && neededWidth > maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
            } else {
                currentRow.add(placeable)
                currentRowWidth = neededWidth
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalWidth = if (constraints.hasFixedWidth) {
            constraints.maxWidth
        } else {
            val widestRow = rows.maxOfOrNull { row ->
                row.sumOf { it.width } + (row.size - 1).coerceAtLeast(0) * hSpacePx
            } ?: 0
            widestRow.coerceIn(constraints.minWidth, constraints.maxWidth)
        }
        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: 0 }
        val totalHeight = (rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * vSpacePx)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(totalWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                val rowHeight = rowHeights[rowIndex]
                var x = 0
                row.forEach { placeable ->
                    val yOffset = (rowHeight - placeable.height) / 2
                    placeable.placeRelative(x, y + yOffset)
                    x += placeable.width + hSpacePx
                }
                y += rowHeight + vSpacePx
            }
        }
    }
}

@Composable
private fun VerifiedAccountBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = VerifiedEmerald.copy(alpha = 0.14f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_badge_check),
                contentDescription = "Verified account",
                tint = VerifiedEmerald,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Verified",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = VerifiedEmerald,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CategoryLabelPill(
    text: String,
    accentColor: Color = WantedBlue
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Permanently text-only full-width Wanted Request card.
 * STRICT RULE: Never displays listing photographs, thumbnails, image placeholders,
 * missing-image icons, or empty image containers anywhere in the app.
 */
@Composable
fun WantedTextOnlyCard(
    listing: Listing,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onContactClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onSellerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val requesterName = listing.sellerName?.takeIf { it.isNotBlank() } ?: "MarketZed User"
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wanted_text_only_card_${listing.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WrapRow(
                    modifier = Modifier.weight(1f),
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 4.dp
                ) {
                    StatusBadgePill("WANTED", WantedBlue, Color.White)
                    if (listing.subcategory.isNotBlank()) {
                        CategoryLabelPill(listing.subcategory, WantedBlue)
                    }
                    if (listing.sellerVerified) {
                        VerifiedAccountBadge()
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("favorite_toggle_${listing.id}")
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_heart),
                        contentDescription = if (isFavorite) "Remove ${listing.title} from saved" else "Save ${listing.title}",
                        tint = if (isFavorite) MarketZedOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = listing.title.ifBlank { "Wanted request" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (listing.description.isNotBlank()) {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            WrapRow(
                horizontalSpacing = 6.dp,
                verticalSpacing = 4.dp
            ) {
                listing.price?.takeIf { it > 0.0 }?.let { budget ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MarketZedOrange.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Budget: ${PriceFormatter.formatKwachaAmount(budget)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MarketZedOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = listing.location?.displayFull ?: "Zambia",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                listing.categoryDetails?.wantedDetails?.takeIf { it.isNotBlank() }?.let { attrs ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = attrs,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .then(if (onSellerClick != null) Modifier.clickable(onClick = onSellerClick) else Modifier)
                ) {
                    UserAvatarBadge(
                        photoUrl = listing.sellerPhotoUrl,
                        displayName = requesterName,
                        size = 28.dp
                    )
                    Column {
                        Text(
                            text = requesterName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = DateUtils.formatRelativeTime(listing.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onReportClick != null) {
                        TextButton(
                            onClick = onReportClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("wanted_report_${listing.id}")
                        ) {
                            Text("Report", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WantedBlue.copy(alpha = 0.14f),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onContactClick ?: onClick)
                            .testTag("wanted_contact_${listing.id}")
                    ) {
                        Text(
                            text = if (onContactClick != null) "Contact" else "View",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WantedBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Text-only shimmer skeleton specifically for Wanted Requests (no image skeleton block).
 */
@Composable
fun WantedTextOnlySkeletonSection(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wanted_text_only_skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-width single-column Jobs card WITH image support (uploaded photo or clean fallback).
 */
@Composable
fun JobFullWidthCard(
    listing: Listing,
    isFavorite: Boolean,
    dataSaverEnabled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onContactClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onSellerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firstImg = listing.images.firstOrNull()
    val imageUrl = CloudinaryUrlHelper.optimizeForFeed(
        url = firstImg?.url,
        thumbUrl = firstImg?.thumbUrl,
        dataSaverEnabled = dataSaverEnabled
    )
    val employmentLabel = listing.categoryDetails?.employmentType?.takeIf { it.isNotBlank() }
        ?: listing.subcategory.takeIf { it.isNotBlank() }
        ?: "Opportunity"
    val employerName = listing.sellerName?.takeIf { it.isNotBlank() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("job_full_width_card_${listing.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Job image banner (image-supported, 100% width)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { Box(modifier = Modifier.fillMaxSize().background(rememberShimmerBrush())) },
                        error = { ListingPlaceholderArt(categoryId = "jobs") }
                    )
                } else {
                    ListingPlaceholderArt(categoryId = "jobs")
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .testTag("favorite_toggle_${listing.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isFavorite) MarketZedOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_heart),
                                contentDescription = if (isFavorite) "Remove ${listing.title} from saved" else "Save ${listing.title}",
                                tint = if (isFavorite) MarketZedOrange else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WrapRow(
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 4.dp
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WantedBlue.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_briefcase),
                                contentDescription = null,
                                tint = WantedBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = employmentLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WantedBlue
                            )
                        }
                    }
                    if (listing.isPromoted) {
                        StatusBadgePill("Sponsored", MarketZedOrange, Color.White)
                    }
                    if (listing.sellerVerified) {
                        VerifiedAccountBadge()
                    }
                }

                Text(
                    text = listing.title.ifBlank { "Job Opportunity" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (employerName != null) {
                    Text(
                        text = employerName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MarketZedOrange,
                        modifier = if (onSellerClick != null) Modifier.clickable(onClick = onSellerClick) else Modifier
                    )
                }

                if (listing.description.isNotBlank()) {
                    Text(
                        text = listing.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listing.price?.takeIf { it > 0.0 }?.let { salary ->
                            Text(
                                text = PriceFormatter.formatListingPrice(salary, listing.priceMode, "jobs"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${listing.location?.displayFull ?: "Zambia"} · ${DateUtils.formatRelativeTime(listing.createdAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (onReportClick != null) {
                            TextButton(
                                onClick = onReportClick,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Report", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (onContactClick != null) {
                            OutlinedButton(
                                onClick = onContactClick,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Contact", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MarketZedOrange.copy(alpha = 0.14f),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onClick)
                        ) {
                            Text(
                                text = "View details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MarketZedOrange,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-width single-column Services card WITH service image support.
 * Uses WrapRow for badges so "Verified" never truncates cleanly beside longer subcategory chips.
 */
@Composable
fun ServiceFullWidthCard(
    listing: Listing,
    isFavorite: Boolean,
    dataSaverEnabled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onContactClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onSellerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firstImg = listing.images.firstOrNull()
    val imageUrl = CloudinaryUrlHelper.optimizeForFeed(
        url = firstImg?.url,
        thumbUrl = firstImg?.thumbUrl,
        dataSaverEnabled = dataSaverEnabled
    )
    val serviceCategory = listing.subcategory.takeIf { it.isNotBlank() } ?: "Services"
    val providerName = listing.sellerName?.takeIf { it.isNotBlank() } ?: "Service Provider"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_full_width_card_${listing.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = imageUrl,
                            contentDescription = listing.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { Box(modifier = Modifier.fillMaxSize().background(rememberShimmerBrush())) },
                            error = { ListingPlaceholderArt(categoryId = "services") }
                        )
                    } else {
                        ListingPlaceholderArt(categoryId = "services")
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WrapRow(
                            modifier = Modifier.weight(1f),
                            horizontalSpacing = 6.dp,
                            verticalSpacing = 4.dp
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = WantedBlue.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_wrench),
                                        contentDescription = null,
                                        tint = WantedBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = serviceCategory,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WantedBlue,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (listing.isPromoted) {
                                StatusBadgePill("Sponsored", MarketZedOrange, Color.White)
                            }
                            if (listing.sellerVerified) {
                                VerifiedAccountBadge()
                            }
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("favorite_toggle_${listing.id}")
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_heart),
                                contentDescription = if (isFavorite) "Remove ${listing.title} from saved" else "Save ${listing.title}",
                                tint = if (isFavorite) MarketZedOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = listing.title.ifBlank { "Local Service" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = providerName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MarketZedOrange,
                        modifier = if (onSellerClick != null) Modifier.clickable(onClick = onSellerClick) else Modifier
                    )
                }
            }

            if (listing.description.isNotBlank()) {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listing.price?.takeIf { it > 0.0 }?.let { price ->
                        Text(
                            text = "From ${PriceFormatter.formatKwachaAmount(price)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${listing.location?.displayFull ?: "Zambia"} · ${DateUtils.formatRelativeTime(listing.createdAtMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onReportClick != null) {
                        TextButton(
                            onClick = onReportClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Report", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (onContactClick != null) {
                        OutlinedButton(
                            onClick = onContactClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Contact", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MarketZedOrange.copy(alpha = 0.14f),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onClick)
                    ) {
                        Text(
                            text = "View details",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MarketZedOrange,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-width marketplace card for Explore (Products, Rental Properties, Swaps, Free).
 * Enforces full-width single-column Explore specification with image, condition/rental info,
 * verified price drop pill, genuine Verified badge, Compare button, and Seller link.
 */
@Composable
fun FullWidthMarketplaceCard(
    listing: Listing,
    isFavorite: Boolean,
    isCompared: Boolean,
    dataSaverEnabled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompare: (() -> Unit)? = null,
    onSellerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val firstImg = listing.images.firstOrNull()
    val imageUrl = CloudinaryUrlHelper.optimizeForFeed(
        url = firstImg?.url,
        thumbUrl = firstImg?.thumbUrl,
        dataSaverEnabled = dataSaverEnabled
    )
    val isProperty = listing.categoryId.equals("property", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("full_width_marketplace_card_${listing.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { Box(modifier = Modifier.fillMaxSize().background(rememberShimmerBrush())) },
                        error = { ListingPlaceholderArt(categoryId = listing.categoryId) }
                    )
                } else {
                    ListingPlaceholderArt(categoryId = listing.categoryId)
                }

                WrapRow(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 4.dp
                ) {
                    if (listing.isPromoted) {
                        StatusBadgePill("Sponsored", MarketZedOrange, Color.White)
                    }
                    if (isProperty) {
                        StatusBadgePill("For Rent", VerifiedEmerald, Color.White)
                    }
                    listing.priceDropPercent?.let { pct ->
                        StatusBadgePill("Save $pct%", VerifiedEmerald, Color.White)
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .testTag("favorite_toggle_${listing.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isFavorite) MarketZedOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_heart),
                                contentDescription = if (isFavorite) "Remove ${listing.title} from saved" else "Save ${listing.title}",
                                tint = if (isFavorite) MarketZedOrange else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WrapRow(
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 4.dp
                ) {
                    CategoryLabelPill(
                        text = buildString {
                            append(MarketZedCatalogue.categoryLabel(listing.categoryId))
                            if (listing.subcategory.isNotBlank()) append(" · ${listing.subcategory}")
                        },
                        accentColor = MarketZedOrange
                    )
                    listing.categoryDetails?.condition?.takeIf { it.isNotBlank() }?.let { cond ->
                        CategoryLabelPill(cond, WantedBlue)
                    }
                    listing.categoryDetails?.bedrooms?.takeIf { it.isNotBlank() }?.let { beds ->
                        CategoryLabelPill("$beds Bedrooms", VerifiedEmerald)
                    }
                    if (listing.sellerVerified) {
                        VerifiedAccountBadge()
                    }
                }

                Text(
                    text = listing.title.ifBlank { "Untitled listing" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = PriceFormatter.formatListingPrice(
                            price = listing.price,
                            priceMode = listing.priceMode,
                            categoryId = listing.categoryId
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MarketZedOrange
                    )
                    if (listing.hasVerifiedPriceDrop && listing.originalPrice != null) {
                        Text(
                            text = PriceFormatter.formatKwachaAmount(listing.originalPrice),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listing.sellerName?.takeIf { it.isNotBlank() }?.let { seller ->
                            Text(
                                text = seller,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (onSellerClick != null) Modifier.clickable(onClick = onSellerClick) else Modifier
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${listing.location?.displayFull ?: "Zambia"} · ${DateUtils.formatRelativeTime(listing.createdAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (onToggleCompare != null && listing.isComparableCategory) {
                            ListingCompareToggleButton(
                                listing = listing,
                                isCompared = isCompared,
                                onToggle = onToggleCompare
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MarketZedOrange.copy(alpha = 0.14f),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onClick)
                        ) {
                            Text(
                                text = "View details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MarketZedOrange,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dispatcher ensuring every screen renders the exact category-compliant full-width card
 * (Wanted = strictly text-only, Jobs = full-width + image, Services = full-width + image,
 * Products/Rentals/Swaps = full-width marketplace card).
 */
@Composable
fun CategoryAwareFullWidthListingCard(
    listing: Listing,
    isFavorite: Boolean,
    isCompared: Boolean,
    dataSaverEnabled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompare: (() -> Unit)? = null,
    onContactClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onSellerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when {
        listing.isWanted -> WantedTextOnlyCard(
            listing = listing,
            isFavorite = isFavorite,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            onContactClick = onContactClick,
            onReportClick = onReportClick,
            onSellerClick = onSellerClick,
            modifier = modifier
        )
        listing.categoryId.equals("jobs", ignoreCase = true) -> JobFullWidthCard(
            listing = listing,
            isFavorite = isFavorite,
            dataSaverEnabled = dataSaverEnabled,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            onContactClick = onContactClick,
            onReportClick = onReportClick,
            onSellerClick = onSellerClick,
            modifier = modifier
        )
        listing.categoryId.equals("services", ignoreCase = true) -> ServiceFullWidthCard(
            listing = listing,
            isFavorite = isFavorite,
            dataSaverEnabled = dataSaverEnabled,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            onContactClick = onContactClick,
            onReportClick = onReportClick,
            onSellerClick = onSellerClick,
            modifier = modifier
        )
        else -> FullWidthMarketplaceCard(
            listing = listing,
            isFavorite = isFavorite,
            isCompared = isCompared,
            dataSaverEnabled = dataSaverEnabled,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            onToggleCompare = onToggleCompare,
            onSellerClick = onSellerClick,
            modifier = modifier
        )
    }
}

/**
 * Quick Contact modal usable directly from Wanted, Jobs, Services, Compare, or Seller screens
 * using ONLY published contact methods from [Listing.contactPreferences].
 */
@Composable
fun QuickContactListingDialog(
    listing: Listing,
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)? = null,
    onOpenSellerProfile: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = contextSensitiveContactLabel(listing.categoryId, listing.sellerAccountType),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                listing.sellerName?.takeIf { it.isNotBlank() }?.let { seller ->
                    Text(
                        text = seller,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerticalContactActionsColumn(listing = listing)
                if (onOpenSellerProfile != null && !listing.sellerName.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenSellerProfile()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Public Seller Storefront", color = MarketZedOrange)
                    }
                }
                if (onViewDetails != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onViewDetails()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Full Listing Details")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
