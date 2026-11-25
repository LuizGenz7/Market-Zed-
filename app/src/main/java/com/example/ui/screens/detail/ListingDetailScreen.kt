package com.example.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.BuildConfig
import com.example.core.util.CloudinaryUrlHelper
import com.example.core.util.DateUtils
import com.example.core.util.PhoneValidator
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.ui.components.ErrorStateView
import com.example.ui.components.ListingCard
import com.example.ui.components.ListingCompareToggleButton
import com.example.ui.components.ListingDetailSkeleton
import com.example.ui.components.ListingPlaceholderArt
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.StatusBadgePill
import com.example.ui.components.TopSnackbarController
import com.example.ui.components.UserAvatarBadge
import com.example.ui.components.VerticalContactActionsColumn
import com.example.ui.components.WantedTextOnlyCard
import com.example.ui.components.WrapRow
import com.example.ui.components.contextSensitiveContactLabel
import com.example.ui.components.rememberShimmerBrush
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WantedBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    state: ListingDetailUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequireSignInForReport: () -> Unit,
    onSubmitReport: (reason: String, details: String) -> Unit,
    onClearReportFeedback: () -> Unit,
    isCompared: Boolean = false,
    onToggleCompare: () -> Unit = {},
    onOpenListing: (String) -> Unit = {},
    onOpenSeller: (Listing) -> Unit = {},
    onToggleSimilarFavorite: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbar = LocalTopSnackbarController.current
    var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.reportResultMessage) {
        val msg = state.reportResultMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (msg.contains("thank", ignoreCase = true) || msg.contains("submitted", ignoreCase = true)) {
            showReportDialog = false
            snackbar?.showSuccess(msg)
        } else {
            snackbar?.showWarning(msg)
        }
        onClearReportFeedback()
    }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hasListing = !state.isLoading && state.listing != null
    val isWanted = state.listing?.isWanted == true
    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val maxHeroHeight = if (state.listing?.images?.isNotEmpty() == true) 290.dp else 200.dp
    val maxHeroHeightPx = with(density) { maxHeroHeight.toPx() }
    val minHeroHeightPx = with(density) { 88.dp.toPx() } + statusBarTopPx
    val maxCollapsePx = (maxHeroHeightPx - minHeroHeightPx).coerceAtLeast(1f)
    var heroCollapseOffsetPx by remember(state.listing?.id, maxCollapsePx) {
        mutableFloatStateOf(0f)
    }
    val nestedScrollConnection = remember(maxCollapsePx, scrollState, isWanted) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isWanted) return Offset.Zero
                val deltaY = available.y
                if (deltaY < 0f && heroCollapseOffsetPx < maxCollapsePx) {
                    val prev = heroCollapseOffsetPx
                    val next = (prev - deltaY).coerceIn(0f, maxCollapsePx)
                    heroCollapseOffsetPx = next
                    return Offset(0f, -(next - prev))
                }
                if (deltaY > 0f && scrollState.value == 0 && heroCollapseOffsetPx > 0f) {
                    val prev = heroCollapseOffsetPx
                    val next = (prev - deltaY).coerceIn(0f, maxCollapsePx)
                    heroCollapseOffsetPx = next
                    return Offset(0f, prev - next)
                }
                return Offset.Zero
            }
        }
    }

    val currentHeroHeightDp = with(density) {
        (maxHeroHeightPx - heroCollapseOffsetPx).coerceAtLeast(minHeroHeightPx).toDp()
    }
    val collapseFraction by remember(maxCollapsePx) {
        derivedStateOf {
            (heroCollapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
        }
    }
    val overSliver = hasListing && !isWanted && collapseFraction < 0.72f
    val buttonPillBg by animateColorAsState(
        targetValue = if (overSliver) {
            Color.Black.copy(alpha = 0.36f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        },
        label = "detailAppBarPillBg"
    )
    val buttonIconTint by animateColorAsState(
        targetValue = if (overSliver) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "detailAppBarIconTint"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = when {
            !hasListing -> 1f
            isWanted -> 1f
            else -> ((collapseFraction - 0.55f) / 0.35f).coerceIn(0f, 1f)
        },
        label = "detailAppBarTitleAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                ListingDetailSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp)
                )
            }
            state.listing == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorStateView(
                        title = "Listing unavailable",
                        message = state.errorMessage ?: "This listing may have been removed or is not cached offline.",
                        retryLabel = "Go back",
                        onRetry = onBack
                    )
                }
            }
            else -> {
                val listing = state.listing
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    // STRICT RULE: Wanted requests are permanently text-only — zero hero photo/placeholder!
                    if (listing.isWanted) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .height(68.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(currentHeroHeightDp)
                                .clipToBounds()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(1f - collapseFraction * 0.25f)
                            ) {
                                if (listing.images.isNotEmpty()) {
                                    val pagerState = rememberPagerState(pageCount = { listing.images.size })
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        val img = listing.images[page]
                                        val url = CloudinaryUrlHelper.optimizeForDetail(
                                            url = img.url,
                                            dataSaverEnabled = state.settings.dataSaverEnabled
                                        )
                                        val shimmer = rememberShimmerBrush()
                                        SubcomposeAsyncImage(
                                            model = url,
                                            contentDescription = "${listing.title} photo ${page + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { fullscreenImageIndex = page },
                                            loading = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(shimmer)
                                                )
                                            },
                                            error = {
                                                ListingPlaceholderArt(
                                                    categoryId = listing.categoryId,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        )
                                    }
                                    if (listing.images.size > 1 && collapseFraction < 0.65f) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(end = 14.dp, bottom = 24.dp)
                                        ) {
                                            Text(
                                                text = "${pagerState.currentPage + 1} / ${listing.images.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                } else {
                                    ListingPlaceholderArt(
                                        categoryId = listing.categoryId,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.42f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                    )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Badges Row (WrapRow so badges never truncate)
                        WrapRow(
                            horizontalSpacing = 8.dp,
                            verticalSpacing = 6.dp
                        ) {
                            if (listing.isPromoted) {
                                StatusBadgePill("Sponsored", MarketZedOrange, Color.White)
                            }
                            if (listing.categoryId.equals("property", ignoreCase = true)) {
                                StatusBadgePill("For Rent", VerifiedEmerald, Color.White)
                            }
                            listing.priceDropPercent?.let { pct ->
                                StatusBadgePill("Save $pct%", VerifiedEmerald, Color.White)
                            }
                            if (listing.isWanted) {
                                StatusBadgePill("WANTED", WantedBlue, Color.White)
                            }
                            if (listing.sellerVerified) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VerifiedEmerald.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(LucideR.drawable.lucide_ic_badge_check),
                                            contentDescription = "Verified account",
                                            tint = VerifiedEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Verified Account",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VerifiedEmerald,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Title & Price + Compare toggle
                        Text(
                            text = listing.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = PriceFormatter.formatListingPrice(
                                        price = listing.price,
                                        priceMode = listing.priceMode,
                                        categoryId = listing.categoryId
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MarketZedOrange,
                                    fontWeight = FontWeight.Bold
                                )
                                listing.originalPrice?.takeIf { listing.hasVerifiedPriceDrop }?.let { orig ->
                                    Text(
                                        text = "Was ${PriceFormatter.formatKwachaAmount(orig)}",
                                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (listing.isComparableCategory) {
                                ListingCompareToggleButton(
                                    listing = listing,
                                    isCompared = isCompared,
                                    onToggle = onToggleCompare
                                )
                            }
                        }

                        // Metadata
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailMetaRow(
                                iconRes = LucideR.drawable.lucide_ic_map_pin,
                                text = listing.location?.displayFull ?: "Location not specified"
                            )
                            DetailMetaRow(
                                iconRes = LucideR.drawable.lucide_ic_calendar,
                                text = "Posted ${DateUtils.formatDateLusaka(listing.createdAtMillis)} · ${MarketZedCatalogue.categoryLabel(listing.categoryId)}" +
                                    if (listing.subcategory.isNotBlank()) " / ${listing.subcategory}" else ""
                            )
                        }

                        // Seller / Requester Storefront Card
                        val sellerLabel = listing.sellerName?.takeIf { it.isNotBlank() } ?: "MarketZed User"
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSeller(listing) }
                                .testTag("detail_seller_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                UserAvatarBadge(
                                    photoUrl = listing.sellerPhotoUrl,
                                    displayName = sellerLabel,
                                    size = 44.dp
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = sellerLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (listing.isWanted) "View requester listings" else "View Seller Storefront",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MarketZedOrange,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                                    contentDescription = "Open seller storefront",
                                    tint = MarketZedOrange
                                )
                            }
                        }

                        // Category-specific attributes & recorded Specifications
                        val details = listing.categoryDetails
                        val detailPairs = buildList {
                            if (listing.categoryId.equals("property", ignoreCase = true)) {
                                add("Transaction" to "For Rent only")
                            }
                            details?.employerName?.takeIf { it.isNotBlank() }?.let { add("Employer" to it) }
                            details?.condition?.takeIf { it.isNotBlank() }?.let { add("Condition" to it) }
                            details?.bedrooms?.takeIf { it.isNotBlank() }?.let { add("Bedrooms" to it) }
                            details?.bathrooms?.takeIf { it.isNotBlank() }?.let { add("Bathrooms" to it) }
                            details?.amenities?.takeIf { it.isNotBlank() }?.let { add("Amenities" to it) }
                            details?.employmentType?.takeIf { it.isNotBlank() }?.let { add("Employment type" to it) }
                            details?.swapFor?.takeIf { it.isNotBlank() }?.let { add("Swap for" to it) }
                            details?.serviceArea?.takeIf { it.isNotBlank() }?.let { add("Service area" to it) }
                            details?.wantedDetails?.takeIf { it.isNotBlank() }?.let { add("Wanted details" to it) }
                        }
                        if (detailPairs.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Details",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    detailPairs.forEach { (k, v) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        if (listing.resolvedSpecifications.isNotEmpty() || listing.isComparableCategory) {
                            val specs = remember(listing.id, listing.resolvedSpecifications) {
                                MarketZedCatalogue.extractComparisonSpecs(listing)
                            }
                            if (specs.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Specifications",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        specs.forEach { (k, v) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = v,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (v == "Not provided") FontWeight.Normal else FontWeight.SemiBold,
                                                    color = if (v == "Not provided") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Description
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = listing.description.ifBlank { "No description provided." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Contact Preferences Section (ONLY published methods, strictly vertical stack)
                        val prefs = listing.contactPreferences
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = contextSensitiveContactLabel(listing.categoryId, listing.sellerAccountType),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                VerticalContactActionsColumn(listing = listing)
                            }
                        }

                        // Safety Reminder
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_shield),
                                    contentDescription = null,
                                    tint = MarketZedOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Safety tip: Inspect items in person in a safe public place. Never pay deposits before verifying, and never share mobile-money PINs or OTPs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Report Listing Action
                        OutlinedButton(
                            onClick = {
                                if (state.isSignedIn) {
                                    showReportDialog = true
                                } else {
                                    onRequireSignInForReport()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("detail_report_button")
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_flag),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isSignedIn) "Report this listing" else "Sign in to report listing")
                        }

                        // Similar Listings Section
                        if (state.similarProducts.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (listing.isWanted) "Similar Wanted Requests" else "Similar Listings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (listing.isWanted) {
                                    state.similarProducts.forEach { sim ->
                                        WantedTextOnlyCard(
                                            listing = sim,
                                            isFavorite = false,
                                            onClick = { onOpenListing(sim.id) },
                                            onToggleFavorite = { onToggleSimilarFavorite(sim.id) },
                                            onSellerClick = { onOpenSeller(sim) }
                                        )
                                    }
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(state.similarProducts, key = { "sim_${it.id}" }) { sim ->
                                            if (sim.isWanted) {
                                                Box(modifier = Modifier.width(288.dp)) {
                                                    WantedTextOnlyCard(
                                                        listing = sim,
                                                        isFavorite = false,
                                                        onClick = { onOpenListing(sim.id) },
                                                        onToggleFavorite = { onToggleSimilarFavorite(sim.id) },
                                                        onSellerClick = { onOpenSeller(sim) }
                                                    )
                                                }
                                            } else {
                                                Box(modifier = Modifier.width(184.dp)) {
                                                    ListingCard(
                                                        listing = sim,
                                                        isFavorite = false,
                                                        dataSaverEnabled = state.settings.dataSaverEnabled,
                                                        onClick = { onOpenListing(sim.id) },
                                                        onToggleFavorite = { onToggleSimilarFavorite(sim.id) },
                                                        onSellerClick = { onOpenSeller(sim) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Floating AppBar
        TopAppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars),
            title = {
                if (titleAlpha > 0.02f) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.alpha(titleAlpha)
                    ) {
                        Text(
                            text = if (isWanted) "Wanted Request" else "Listing Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("detail_back_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = buttonPillBg,
                        border = if (!overSliver) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_arrow_left),
                                contentDescription = "Back",
                                tint = buttonIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            actions = {
                val activeListing = state.listing
                if (activeListing != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_favorite_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = buttonPillBg,
                            border = if (!overSliver) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)) else null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_heart),
                                    contentDescription = if (state.isFavorite) "Remove from saved" else "Save listing",
                                    tint = if (state.isFavorite) MarketZedOrange else buttonIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { shareListing(context, activeListing) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("detail_share_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = buttonPillBg,
                            border = if (!overSliver) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)) else null,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_share_2),
                                    contentDescription = "Share listing",
                                    tint = buttonIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = buttonIconTint,
                actionIconContentColor = buttonIconTint
            )
        )
    }

    // Fullscreen image viewer (never enabled for Wanted)
    val listing = state.listing
    if (fullscreenImageIndex != null && listing != null && !listing.isWanted && listing.images.isNotEmpty()) {
        Dialog(
            onDismissRequest = { fullscreenImageIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val pagerState = rememberPagerState(
                    initialPage = fullscreenImageIndex!!.coerceIn(0, listing.images.lastIndex),
                    pageCount = { listing.images.size }
                )
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val shimmer = rememberShimmerBrush()
                    SubcomposeAsyncImage(
                        model = listing.images[page].url,
                        contentDescription = "Expanded photo ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(shimmer)
                            )
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_image_off),
                                    contentDescription = "Image unavailable",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                }
                IconButton(
                    onClick = { fullscreenImageIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_x),
                            contentDescription = "Close gallery",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportListingDialog(
            isSubmitting = state.reportSubmitting,
            feedbackMessage = state.reportResultMessage,
            onDismiss = {
                onClearReportFeedback()
                showReportDialog = false
            },
            onSubmit = onSubmitReport
        )
    }
}

@Composable
private fun DetailMetaRow(
    @DrawableRes iconRes: Int,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportListingDialog(
    isSubmitting: Boolean,
    feedbackMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(MarketZedCatalogue.reportReasons.first().first) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Listing") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarketZedCatalogue.reportReasons.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = code },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == code,
                            onClick = { selectedReason = code }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                MarketZedOutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 1000) details = it },
                    label = { Text("Additional details (optional, max 1000)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_flag),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!feedbackMessage.isNullOrBlank()) {
                    Text(
                        text = feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MarketZedOrange
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun copyToClipboard(
    context: Context,
    label: String,
    value: String,
    snackbar: TopSnackbarController?
) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
    snackbar?.showSuccess("$label copied to clipboard")
}

private fun shareListing(context: Context, listing: Listing) {
    val base = BuildConfig.MARKETZED_API_BASE_URL.trimEnd('/')
    val url = "$base/listings/${listing.id}"
    val shareText = "${listing.title}\n$url"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, listing.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share listing"))
}
