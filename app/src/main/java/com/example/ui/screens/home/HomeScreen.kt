package com.example.ui.screens.home

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.repository.MarketplaceRepository
import com.example.ui.components.CompactSponsoredStrip
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FlexibleSponsoredCarousel
import com.example.ui.components.FullWidthListingSkeletonSection
import com.example.ui.components.JobFullWidthCard
import com.example.ui.components.ListingCard
import com.example.ui.components.LoadingSkeletonSection
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.MotivationalBanner
import com.example.ui.components.OfflineStaleBanner
import com.example.ui.components.QuickContactListingDialog
import com.example.ui.components.ServiceFullWidthCard
import com.example.ui.components.SponsoredHeroBannerCard
import com.example.ui.components.SubtleRefreshBar
import com.example.ui.components.WantedTextOnlyCard
import com.example.ui.components.WantedTextOnlySkeletonSection
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.MarketZedOrangeDark
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WantedBlue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    unreadNotifications: Int,
    onRefresh: () -> Unit,
    onOpenSearch: (String?) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenExploreAll: () -> Unit,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenPostWanted: () -> Unit,
    onOpenNotifications: () -> Unit,
    onToggleDataSaver: () -> Unit,
    onSubmitTestimonial: (name: String, town: String, message: String, rating: Int) -> Unit,
    onClearTestimonialFeedback: () -> Unit,
    onRotateMotivationalMessage: () -> Unit,
    onClearSyncError: () -> Unit,
    onOpenWantedTab: () -> Unit = { onOpenCategory("wanted") },
    comparedIds: Set<String> = emptySet(),
    onToggleCompare: (Listing) -> Unit = {},
    onOpenSeller: (Listing) -> Unit = {},
    onClearRecentlyViewed: () -> Unit = {},
    onOpenSubcategory: (String, String) -> Unit = { catId, _ -> onOpenCategory(catId) },
    onOpenLocationDiscovery: (String) -> Unit = { town -> onOpenSearch(town) },
    onOpenPostTab: () -> Unit = onOpenPostWanted,
    modifier: Modifier = Modifier
) {
    var showTestimonialDialog by remember { mutableStateOf(false) }
    var quickContactListing by remember { mutableStateOf<Listing?>(null) }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000L)
            onRotateMotivationalMessage()
        }
    }

    LaunchedEffect(state.syncError, state.isOffline) {
        val err = state.syncError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (!state.isOffline) {
            snackbar?.showWarning(
                message = err,
                actionLabel = "Retry",
                onAction = onRefresh
            )
            onClearSyncError()
        }
    }

    LaunchedEffect(state.testimonialFeedback) {
        val feedback = state.testimonialFeedback?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (feedback.startsWith("Thank you", ignoreCase = true)) {
            showTestimonialDialog = false
            snackbar?.showSuccess(feedback)
        } else {
            snackbar?.showWarning(feedback)
        }
        onClearTestimonialFeedback()
    }

    val quickTowns = remember { listOf("Lusaka", "Kitwe", "Ndola", "Livingstone", "Chipata", "Solwezi") }
    val allRentals = remember(state.sponsoredRentals, state.rentalProperties) {
        (state.sponsoredRentals + state.rentalProperties).distinctBy { it.id }
    }
    val allJobs = remember(state.promotedJobs, state.latestJobs) {
        (state.promotedJobs + state.latestJobs).distinctBy { it.id }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubtleRefreshBar(visible = (state.isRefreshing || state.isSyncing) && state.hasAnyMarketplaceContent)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Compact MarketZed Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MarketZedOrange,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_shopping_bag),
                                        contentDescription = "MarketZed logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MarketZed",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Zambia Marketplace",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = state.settings.dataSaverEnabled,
                                onClick = onToggleDataSaver,
                                label = {
                                    Text(
                                        text = if (state.settings.dataSaverEnabled) "Saver On" else "Saver",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_gauge),
                                        contentDescription = "Toggle Data Saver",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("home_data_saver_chip")
                            )

                            IconButton(
                                onClick = onOpenNotifications,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifications > 0) {
                                            Badge(containerColor = MarketZedOrange) {
                                                Text(unreadNotifications.coerceAtMost(99).toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_bell),
                                        contentDescription = "Notifications"
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Search Entry Point
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_bar")
                            .clickable { onOpenSearch(null) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_search),
                                contentDescription = null,
                                tint = MarketZedOrange
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Search products, rentals, services & jobs...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 3. Offline / Stale Banner
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

                // 4. Location Quick Discovery Pills
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickTowns) { town ->
                            AssistChip(
                                onClick = { onOpenLocationDiscovery(town) },
                                label = { Text(town, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                                        contentDescription = null,
                                        tint = MarketZedOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.testTag("home_town_chip_$town")
                            )
                        }
                    }
                }

                // 5. Compact MarketZed Hero
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(MarketZedOrange, MarketZedOrangeDark)
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Buy, sell, swap & hire across Zambia",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Browse verified products, rental homes & commercial spaces, local services, and jobs across all 10 provinces.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onOpenExploreAll,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = MarketZedOrangeDark
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("hero_explore_button")
                                    ) {
                                        Text("Explore", fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = onOpenPostWanted,
                                        border = BorderStroke(1.dp, Color.White),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("hero_post_wanted_button")
                                    ) {
                                        Text("Post Free Wanted")
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Browse Categories (always visible)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "Browse Categories",
                            actionText = "All 7",
                            onAction = onOpenExploreAll
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(MarketZedCatalogue.homeShortcutCategoryIds) { catId ->
                                val cat = MarketZedCatalogue.categoryById(catId)
                                if (cat != null) {
                                    CategoryShortcutTile(
                                        label = cat.name,
                                        iconRes = categoryIconRes(cat.id),
                                        onClick = { onOpenCategory(cat.id) }
                                    )
                                }
                            }
                            item {
                                CategoryShortcutTile(
                                    label = "All Categories",
                                    iconRes = LucideR.drawable.lucide_ic_layout_grid,
                                    onClick = onOpenExploreAll
                                )
                            }
                        }
                    }
                }

                // Motivational Banner
                item {
                    MotivationalBanner(
                        message = state.motivationalMessage,
                        onClick = {
                            val targetCat = state.motivationalMessage.targetCategoryId
                            if (targetCat != null) onOpenCategory(targetCat) else onOpenExploreAll()
                        }
                    )
                }

                // 7. Main [1] [3] [1] Sponsored Carousel + Homepage Sponsored Banner
                if (state.carouselSponsored.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Featured Sponsored",
                            actionText = "Explore",
                            onAction = onOpenExploreAll
                        )
                    }
                    item {
                        FlexibleSponsoredCarousel(
                            listings = state.carouselSponsored,
                            favoriteIds = state.favoriteIds,
                            dataSaverEnabled = state.settings.dataSaverEnabled,
                            onOpenListing = onOpenListing,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                state.homepageBanner?.let { banner ->
                    item {
                        SponsoredHeroBannerCard(
                            banner = banner,
                            dataSaverEnabled = state.settings.dataSaverEnabled,
                            onOpenListing = onOpenListing
                        )
                    }
                }

                // 8. Sponsored Products & Sponsored Deals Strips
                if (state.sponsoredProducts.isNotEmpty()) {
                    item {
                        CompactSponsoredStrip(
                            title = "Sponsored Products",
                            listings = state.sponsoredProducts,
                            favoriteIds = state.favoriteIds,
                            dataSaverEnabled = state.settings.dataSaverEnabled,
                            onOpenListing = onOpenListing,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                if (state.sponsoredDeals.isNotEmpty()) {
                    item {
                        CompactSponsoredStrip(
                            title = "Sponsored Deals",
                            listings = state.sponsoredDeals,
                            favoriteIds = state.favoriteIds,
                            dataSaverEnabled = state.settings.dataSaverEnabled,
                            onOpenListing = onOpenListing,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                // 9. Recently Viewed Strip (with Clear button)
                if (state.recentlyViewed.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recently Viewed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = onClearRecentlyViewed,
                                modifier = Modifier.testTag("home_clear_recent_button")
                            ) {
                                Text("Clear", color = MarketZedOrange, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.recentlyViewed, key = { "home_recent_${it.id}" }) { item ->
                                if (item.isWanted) {
                                    Box(modifier = Modifier.width(296.dp)) {
                                        WantedTextOnlyCard(
                                            listing = item,
                                            isFavorite = item.id in state.favoriteIds,
                                            onClick = { onOpenListing(item.id) },
                                            onToggleFavorite = { onToggleFavorite(item.id) },
                                            onSellerClick = { onOpenSeller(item) }
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.width(184.dp)) {
                                        ListingCard(
                                            listing = item,
                                            isFavorite = item.id in state.favoriteIds,
                                            isCompared = item.id in comparedIds,
                                            dataSaverEnabled = state.settings.dataSaverEnabled,
                                            onClick = { onOpenListing(item.id) },
                                            onToggleFavorite = { onToggleFavorite(item.id) },
                                            onToggleCompare = if (item.isComparableCategory) {
                                                { onToggleCompare(item) }
                                            } else null,
                                            onSellerClick = { onOpenSeller(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 10. Priority 1: Products (Recommended, Recently Added, Price Reduced, Popular Subcategories)
                if (state.recommendedProducts.isNotEmpty() || MarketplaceRepository.SECTION_HOME_PRODUCTS in state.loadingSections) {
                    item {
                        SectionHeader(
                            title = "Recommended Products",
                            actionText = "All products",
                            onAction = { onOpenCategory("marketplace") }
                        )
                    }
                    if (state.recommendedProducts.isEmpty()) {
                        item { LoadingSkeletonSection() }
                    } else {
                        items(state.recommendedProducts.take(8).chunked(2), key = { "rec_${it.joinToString("_") { l -> l.id }}" }) { pair ->
                            TwoColumnListingRow(
                                listings = pair,
                                favoriteIds = state.favoriteIds,
                                comparedIds = comparedIds,
                                dataSaverEnabled = state.settings.dataSaverEnabled,
                                onListingClick = onOpenListing,
                                onToggleFavorite = onToggleFavorite,
                                onToggleCompare = onToggleCompare,
                                onSellerClick = onOpenSeller
                            )
                        }
                    }
                }

                if (state.recentlyAddedProducts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently Added Products",
                            actionText = "See more",
                            onAction = { onOpenCategory("marketplace") }
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.recentlyAddedProducts.take(8), key = { "home_added_${it.id}" }) { listing ->
                                Box(modifier = Modifier.width(184.dp)) {
                                    ListingCard(
                                        listing = listing,
                                        isFavorite = listing.id in state.favoriteIds,
                                        isCompared = listing.id in comparedIds,
                                        dataSaverEnabled = state.settings.dataSaverEnabled,
                                        onClick = { onOpenListing(listing.id) },
                                        onToggleFavorite = { onToggleFavorite(listing.id) },
                                        onToggleCompare = { onToggleCompare(listing) },
                                        onSellerClick = { onOpenSeller(listing) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.priceReducedProducts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Price Reduced",
                            actionText = "See deals",
                            onAction = { onOpenCategory("marketplace") }
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.priceReducedProducts.take(8), key = { "home_drop_${it.id}" }) { listing ->
                                Box(modifier = Modifier.width(184.dp)) {
                                    ListingCard(
                                        listing = listing,
                                        isFavorite = listing.id in state.favoriteIds,
                                        isCompared = listing.id in comparedIds,
                                        dataSaverEnabled = state.settings.dataSaverEnabled,
                                        onClick = { onOpenListing(listing.id) },
                                        onToggleFavorite = { onToggleFavorite(listing.id) },
                                        onToggleCompare = if (listing.isComparableCategory) {
                                            { onToggleCompare(listing) }
                                        } else null,
                                        onSellerClick = { onOpenSeller(listing) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.popularSubcategories.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Popular Product Categories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.popularSubcategories, key = { it.id }) { sub ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { onOpenSubcategory(sub.categoryId, sub.subcategoryFilter) },
                                        label = { Text(sub.label) },
                                        modifier = Modifier.testTag("home_subcat_${sub.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                // 11. Priority 2: Rental Properties (Strictly For Rent Only — Zero Land)
                if (allRentals.isNotEmpty() || MarketplaceRepository.SECTION_HOME_PROPERTY in state.loadingSections) {
                    item {
                        SectionHeader(
                            title = "Rental Properties",
                            actionText = "All rentals",
                            onAction = { onOpenCategory("property") }
                        )
                    }
                    if (allRentals.isEmpty()) {
                        item { LoadingSkeletonSection() }
                    } else {
                        items(allRentals.take(4).chunked(2), key = { "rent_${it.joinToString("_") { l -> l.id }}" }) { pair ->
                            TwoColumnListingRow(
                                listings = pair,
                                favoriteIds = state.favoriteIds,
                                comparedIds = comparedIds,
                                dataSaverEnabled = state.settings.dataSaverEnabled,
                                onListingClick = onOpenListing,
                                onToggleFavorite = onToggleFavorite,
                                onToggleCompare = onToggleCompare,
                                onSellerClick = onOpenSeller
                            )
                        }
                    }
                }

                // 12. Priority 3: Services Across Zambia (Full-width with image)
                if (state.servicesPreview.isNotEmpty() || MarketplaceRepository.SECTION_HOME_SERVICES in state.loadingSections) {
                    item {
                        SectionHeader(
                            title = "Services Across Zambia",
                            actionText = "All services",
                            onAction = { onOpenCategory("services") }
                        )
                    }
                    if (state.servicesPreview.isEmpty()) {
                        item { FullWidthListingSkeletonSection(count = 2) }
                    } else {
                        items(state.servicesPreview.take(3), key = { "home_svc_${it.id}" }) { listing ->
                            ServiceFullWidthCard(
                                listing = listing,
                                isFavorite = listing.id in state.favoriteIds,
                                dataSaverEnabled = state.settings.dataSaverEnabled,
                                onClick = { onOpenListing(listing.id) },
                                onToggleFavorite = { onToggleFavorite(listing.id) },
                                onContactClick = { quickContactListing = listing },
                                onSellerClick = { onOpenSeller(listing) }
                            )
                        }
                    }
                }

                // 13. Priority 4: Swaps
                if (state.swapsPreview.isNotEmpty() || MarketplaceRepository.SECTION_HOME_SWAPS in state.loadingSections) {
                    item {
                        SectionHeader(
                            title = "Swaps & Exchanges",
                            actionText = "All swaps",
                            onAction = { onOpenCategory("swaps") }
                        )
                    }
                    if (state.swapsPreview.isEmpty()) {
                        item { LoadingSkeletonSection() }
                    } else {
                        items(state.swapsPreview.take(4).chunked(2), key = { "swap_${it.joinToString("_") { l -> l.id }}" }) { pair ->
                            TwoColumnListingRow(
                                listings = pair,
                                favoriteIds = state.favoriteIds,
                                comparedIds = comparedIds,
                                dataSaverEnabled = state.settings.dataSaverEnabled,
                                onListingClick = onOpenListing,
                                onToggleFavorite = onToggleFavorite,
                                onToggleCompare = onToggleCompare,
                                onSellerClick = onOpenSeller
                            )
                        }
                    }
                }

                // 14. Priority 5: Wanted Requests (PERMANENTLY TEXT-ONLY, ZERO IMAGES)
                if (state.wantedPreview.isNotEmpty() || MarketplaceRepository.SECTION_HOME_WANTED in state.loadingSections) {
                    item {
                        SectionHeader(
                            title = "Wanted Requests",
                            actionText = "See all",
                            onAction = onOpenWantedTab
                        )
                    }
                    if (state.wantedPreview.isEmpty()) {
                        item { WantedTextOnlySkeletonSection(count = 2) }
                    } else {
                        items(state.wantedPreview.take(2), key = { "home_wanted_${it.id}" }) { listing ->
                            WantedTextOnlyCard(
                                listing = listing,
                                isFavorite = listing.id in state.favoriteIds,
                                onClick = { onOpenListing(listing.id) },
                                onToggleFavorite = { onToggleFavorite(listing.id) },
                                onContactClick = { quickContactListing = listing },
                                onSellerClick = { onOpenSeller(listing) }
                            )
                        }
                    }
                }

                // 15. Priority 6: Jobs & Opportunities Banner + Full-width Job cards (only shown when jobs exist or loading)
                if (allJobs.isNotEmpty() || MarketplaceRepository.SECTION_HOME_JOBS in state.loadingSections) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCategory("jobs") },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MarketZedOrange.copy(alpha = 0.14f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(LucideR.drawable.lucide_ic_briefcase),
                                                contentDescription = null,
                                                tint = MarketZedOrange
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Jobs & Opportunities",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Full-time, gigs, internships & apprenticeships across Zambia",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_arrow_right),
                                    contentDescription = "Browse Jobs",
                                    tint = MarketZedOrange
                                )
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Latest Jobs",
                            actionText = "All jobs",
                            onAction = { onOpenCategory("jobs") }
                        )
                    }
                    if (allJobs.isEmpty()) {
                        item { FullWidthListingSkeletonSection(count = 2) }
                    } else {
                        items(allJobs.take(4), key = { "home_job_${it.id}" }) { listing ->
                            JobFullWidthCard(
                                listing = listing,
                                isFavorite = listing.id in state.favoriteIds,
                                dataSaverEnabled = state.settings.dataSaverEnabled,
                                onClick = { onOpenListing(listing.id) },
                                onToggleFavorite = { onToggleFavorite(listing.id) },
                                onContactClick = { quickContactListing = listing },
                                onSellerClick = { onOpenSeller(listing) }
                            )
                        }
                    }
                }

                // Single clean empty state ONLY if all sections finished and marketplace is truly empty
                if (state.hasLoadedLocalCache && state.loadingSections.isEmpty() && !state.hasAnyMarketplaceContent) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MarketZedOrange.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_empty_marketplace_state")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MarketZedOrange.copy(alpha = 0.14f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(LucideR.drawable.lucide_ic_sparkles),
                                            contentDescription = null,
                                            tint = MarketZedOrange,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "YOUR MARKETPLACE STARTS HERE",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MarketZedOrange
                                )
                                Text(
                                    text = state.emptyStateMessage,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.isOffline) {
                                        "You're offline right now, or no cached listings are saved yet. Connect anytime to sync, or start drafting your first listing."
                                    } else {
                                        "Categories stay ready above while listings load in. Be the first in your town to showcase a product, rental, service, job, or Wanted request."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onOpenPostTab,
                                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_empty_post_first_button")
                                ) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_plus),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Post Your First Listing", fontWeight = FontWeight.SemiBold)
                                }
                                if (state.isOffline || state.syncError != null) {
                                    TextButton(onClick = onRefresh) {
                                        Text("Sync Now", color = MarketZedOrange, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 16. Built for Zambia Trust Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Built for Zambia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TrustInfoCard(
                            iconRes = LucideR.drawable.lucide_ic_shield,
                            iconTint = VerifiedEmerald,
                            title = "Safer marketplace",
                            description = "Payment-backed account badges and listing checks help people trade with confidence."
                        )
                        TrustInfoCard(
                            iconRes = LucideR.drawable.lucide_ic_map_pin,
                            iconTint = MarketZedOrange,
                            title = "Made for Zambia",
                            description = "Browse listings in towns across all 10 provinces, optimized for low mobile data."
                        )
                        TrustInfoCard(
                            iconRes = LucideR.drawable.lucide_ic_phone_call,
                            iconTint = WantedBlue,
                            title = "Connect directly",
                            description = "Contact listing owners directly using their published phone, WhatsApp, or email."
                        )
                    }
                }

                // Testimonials CTA
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share your MarketZed experience",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Approved community testimonials appear on MarketZed after review.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showTestimonialDialog = true },
                                modifier = Modifier.testTag("open_testimonial_button")
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_file_pen),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Review")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    quickContactListing?.let { target ->
        QuickContactListingDialog(
            listing = target,
            onDismiss = { quickContactListing = null },
            onViewDetails = { onOpenListing(target.id) },
            onOpenSellerProfile = { onOpenSeller(target) }
        )
    }

    if (showTestimonialDialog) {
        TestimonialDialog(
            isSubmitting = state.testimonialSubmitting,
            feedbackMessage = state.testimonialFeedback,
            onDismiss = {
                onClearTestimonialFeedback()
                showTestimonialDialog = false
            },
            onSubmit = onSubmitTestimonial
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onAction) {
            Text(actionText, color = MarketZedOrange, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryShortcutTile(
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = label,
                        tint = MarketZedOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TwoColumnListingRow(
    listings: List<Listing>,
    favoriteIds: Set<String>,
    dataSaverEnabled: Boolean,
    onListingClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    comparedIds: Set<String> = emptySet(),
    onToggleCompare: ((Listing) -> Unit)? = null,
    onSellerClick: ((Listing) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listings.forEach { item ->
            Box(modifier = Modifier.weight(1f)) {
                ListingCard(
                    listing = item,
                    isFavorite = item.id in favoriteIds,
                    isCompared = item.id in comparedIds,
                    dataSaverEnabled = dataSaverEnabled,
                    onClick = { onListingClick(item.id) },
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    onToggleCompare = if (onToggleCompare != null && item.isComparableCategory) {
                        { onToggleCompare(item) }
                    } else null,
                    onSellerClick = onSellerClick?.let { cb -> { cb(item) } }
                )
            }
        }
        if (listings.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TrustInfoCard(
    @DrawableRes iconRes: Int,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TestimonialDialog(
    isSubmitting: Boolean,
    feedbackMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Feedback") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MarketZedOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name (2–60 chars)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_user),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                MarketZedOutlinedTextField(
                    value = town,
                    onValueChange = { town = it },
                    label = { Text("Town (optional, defaults to Zambia)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rating:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { rating = star },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_star),
                                contentDescription = "$star stars",
                                tint = if (star <= rating) MarketZedOrange else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                MarketZedOutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your experience (12–500 chars)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_message_square_text),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
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
                onClick = { onSubmit(name, town, message, rating) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@DrawableRes
fun categoryIconRes(categoryId: String): Int {
    return when (categoryId.lowercase()) {
        "property" -> LucideR.drawable.lucide_ic_building_2
        "marketplace" -> LucideR.drawable.lucide_ic_shopping_bag
        "swaps" -> LucideR.drawable.lucide_ic_arrow_left_right
        "free" -> LucideR.drawable.lucide_ic_gift
        "services" -> LucideR.drawable.lucide_ic_wrench
        "jobs" -> LucideR.drawable.lucide_ic_briefcase
        "wanted" -> LucideR.drawable.lucide_ic_handshake
        else -> LucideR.drawable.lucide_ic_layout_grid
    }
}
