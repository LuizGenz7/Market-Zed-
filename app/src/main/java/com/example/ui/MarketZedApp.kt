package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.composables.icons.lucide.R as LucideR
import com.example.AppContainer
import com.example.data.model.Listing
import com.example.data.notifications.AppNavigationTarget
import com.example.data.notifications.NotificationKind
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedBottomBar
import com.example.ui.components.SecondaryPageScaffold
import com.example.ui.components.TopLevelTab
import com.example.ui.components.TopSnackbarController
import com.example.ui.components.TopSnackbarHost
import com.example.ui.components.TopSnackbarType
import com.example.ui.screens.account.AccountDestination
import com.example.ui.screens.account.AccountScreen
import com.example.ui.screens.account.AccountViewModel
import com.example.ui.screens.account.ActivationPaymentScreen
import com.example.ui.screens.account.MyListingsScreen
import com.example.ui.screens.account.NotificationsScreen
import com.example.ui.screens.account.PaymentHistoryScreen
import com.example.ui.screens.account.ProfileEditScreen
import com.example.ui.screens.account.PromotionsScreen
import com.example.ui.screens.account.SettingsAndDiagnosticsScreen
import com.example.ui.screens.account.StaticInfoScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminViewModel
import com.example.ui.screens.compare.ComparisonFloatingBar
import com.example.ui.screens.compare.ComparisonScreen
import com.example.ui.screens.compare.ComparisonViewModel
import com.example.ui.screens.compare.QuickComparisonSheet
import com.example.ui.screens.detail.ListingDetailScreen
import com.example.ui.screens.detail.ListingDetailViewModel
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.explore.ExploreViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.post.PostListingScreen
import com.example.ui.screens.post.PostListingViewModel
import com.example.ui.screens.saved.SavedScreen
import com.example.ui.screens.saved.SavedViewModel
import com.example.ui.screens.seller.SellerProfileScreen
import com.example.ui.screens.seller.SellerProfileViewModel
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.wanted.WantedScreen
import com.example.ui.screens.wanted.WantedViewModel
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.MarketZedTheme
import com.example.ui.theme.ThemeMode

private const val ROUTE_DETAIL = "detail/{listingId}"
private const val ROUTE_COMPARE = "compare"
private const val ROUTE_SELLER = "seller/{sellerKey}?listingId={listingId}"

@Composable
fun MarketZedApp(
    container: AppContainer,
    pendingNavigationTarget: AppNavigationTarget?,
    onConsumeNavigationTarget: () -> Unit
) {
    val factory = remember(container) { MarketZedViewModelFactory(container) }
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val exploreViewModel: ExploreViewModel = viewModel(factory = factory)
    val savedViewModel: SavedViewModel = viewModel(factory = factory)
    val wantedViewModel: WantedViewModel = viewModel(factory = factory)
    val postViewModel: PostListingViewModel = viewModel(factory = factory)
    val accountViewModel: AccountViewModel = viewModel(factory = factory)
    val adminViewModel: AdminViewModel = viewModel(factory = factory)
    val comparisonViewModel: ComparisonViewModel = viewModel(factory = factory)

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val exploreState by exploreViewModel.uiState.collectAsStateWithLifecycle()
    val savedState by savedViewModel.uiState.collectAsStateWithLifecycle()
    val wantedState by wantedViewModel.uiState.collectAsStateWithLifecycle()
    val postState by postViewModel.uiState.collectAsStateWithLifecycle()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()
    val comparisonState by comparisonViewModel.uiState.collectAsStateWithLifecycle()

    val themeMode = remember(accountState.settings.themeModeName) {
        when (accountState.settings.themeModeName.uppercase()) {
            "DARK" -> ThemeMode.DARK
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.LIGHT
        }
    }

    MarketZedTheme(themeMode = themeMode) {
        var showSplash by rememberSaveable { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen(onSplashFinished = { showSplash = false })
            return@MarketZedTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        var accountDestination by rememberSaveable { mutableStateOf(AccountDestination.DASHBOARD) }
        var preselectedPromoListing by remember { mutableStateOf<Listing?>(null) }
        var pendingReturnToPost by rememberSaveable { mutableStateOf(false) }
        var activeAnnouncementDialog by remember { mutableStateOf<AppNavigationTarget.Announcement?>(null) }
        val topSnackbarController = remember { TopSnackbarController() }

        fun navigateTopLevel(tab: TopLevelTab) {
            if (tab != TopLevelTab.ACCOUNT) {
                accountDestination = AccountDestination.DASHBOARD
            }
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        fun openListingDetail(listingId: String) {
            if (listingId.isBlank()) return
            val encoded = Uri.encode(listingId)
            navController.navigate("detail/$encoded")
        }

        fun openExploreWithFilter(categoryId: String?, query: String? = null) {
            exploreViewModel.applyInitialCategory(categoryId = categoryId, initialQuery = query)
            navigateTopLevel(TopLevelTab.EXPLORE)
        }

        fun openExploreWithSubcategory(categoryId: String, subcategory: String) {
            exploreViewModel.applySubcategoryFilter(categoryId = categoryId, subcategory = subcategory)
            navigateTopLevel(TopLevelTab.EXPLORE)
        }

        fun openComparison() {
            navController.navigate(ROUTE_COMPARE) {
                launchSingleTop = true
            }
        }

        fun openSellerProfile(listing: Listing) {
            val key = listing.ownerId?.takeIf { it.isNotBlank() }
                ?: listing.sellerName?.takeIf { it.isNotBlank() }
                ?: "member"
            val encodedKey = Uri.encode(key)
            val encodedListing = Uri.encode(listing.id)
            navController.navigate("seller/$encodedKey?listingId=$encodedListing") {
                launchSingleTop = true
            }
        }

        fun handleNavigationTarget(target: AppNavigationTarget) {
            when (target) {
                is AppNavigationTarget.ListingDetail -> openListingDetail(target.listingId)
                is AppNavigationTarget.Category -> openExploreWithFilter(target.categoryId)
                is AppNavigationTarget.Explore -> openExploreWithFilter(categoryId = null, query = target.query)
                is AppNavigationTarget.Notifications -> {
                    accountDestination = AccountDestination.NOTIFICATIONS
                    navigateTopLevel(TopLevelTab.ACCOUNT)
                }
                is AppNavigationTarget.Promotions -> {
                    accountDestination = AccountDestination.PROMOTIONS
                    navigateTopLevel(TopLevelTab.ACCOUNT)
                }
                is AppNavigationTarget.Wanted -> {
                    navigateTopLevel(TopLevelTab.WANTED)
                }
                is AppNavigationTarget.Account -> {
                    accountDestination = AccountDestination.DASHBOARD
                    navigateTopLevel(TopLevelTab.ACCOUNT)
                }
                is AppNavigationTarget.Home -> {
                    navigateTopLevel(TopLevelTab.HOME)
                }
                is AppNavigationTarget.Announcement -> {
                    accountViewModel.markAnnouncementRead(target.id)
                    when {
                        !target.listingId.isNullOrBlank() -> openListingDetail(target.listingId)
                        !target.categoryId.isNullOrBlank() -> openExploreWithFilter(target.categoryId)
                        else -> activeAnnouncementDialog = target
                    }
                }
            }
        }

        // Handle deep links & notification taps once Navigation host is ready
        LaunchedEffect(pendingNavigationTarget) {
            val target = pendingNavigationTarget ?: return@LaunchedEffect
            handleNavigationTarget(target)
            onConsumeNavigationTarget()
        }

        // Collect foreground push messages into animated TopSnackbar
        LaunchedEffect(container) {
            container.notificationPreferencesRepository.foregroundNotices.collect { notice ->
                val snackbarType = when (notice.kind) {
                    NotificationKind.PROMOTION -> TopSnackbarType.INFO
                    NotificationKind.ANNOUNCEMENT -> TopSnackbarType.INFO
                    NotificationKind.ENGAGEMENT -> TopSnackbarType.INFO
                    NotificationKind.GENERAL -> TopSnackbarType.SUCCESS
                }
                val summary = if (notice.title.isNotBlank() && notice.message != notice.title) {
                    "${notice.title}: ${notice.message}"
                } else {
                    notice.message.ifBlank { notice.title }
                }
                topSnackbarController.show(
                    message = summary,
                    type = snackbarType,
                    actionLabel = if (notice.target != null) "Open" else null,
                    onAction = notice.target?.let { tgt -> { handleNavigationTarget(tgt) } }
                )
            }
        }

        // Show comparison feedback toast unless the 3-item AlertDialog is handling it
        LaunchedEffect(comparisonState.feedbackMessage, comparisonState.showLimitAlert) {
            val msg = comparisonState.feedbackMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
            if (!comparisonState.showLimitAlert) {
                topSnackbarController.showInfo(msg)
                comparisonViewModel.dismissFeedback()
            }
        }

        // Return smoothly to Post tab after completing sign-in
        LaunchedEffect(accountState.auth.isSignedIn, pendingReturnToPost) {
            if (accountState.auth.isSignedIn && pendingReturnToPost) {
                pendingReturnToPost = false
                navigateTopLevel(TopLevelTab.POST)
            }
        }

        // Clear admin state immediately if signed out or admin privileges absent
        LaunchedEffect(accountState.isAdmin) {
            if (!accountState.isAdmin) {
                adminViewModel.clearState()
                if (accountDestination == AccountDestination.ADMIN_DASHBOARD) {
                    accountDestination = AccountDestination.DASHBOARD
                }
            }
        }

        val showTopLevelNav = when {
            currentRoute == null -> true
            currentRoute.startsWith("detail/") -> false
            currentRoute == ROUTE_COMPARE -> false
            currentRoute.startsWith("seller/") -> false
            currentRoute == TopLevelTab.ACCOUNT.route && accountDestination != AccountDestination.DASHBOARD -> false
            else -> true
        }

        CompositionLocalProvider(LocalTopSnackbarController provides topSnackbarController) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 840.dp
                val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (showTopLevelNav && !useNavigationRail && !imeVisible) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (comparisonState.count > 0) {
                                    ComparisonFloatingBar(
                                        state = comparisonState,
                                        onOpenQuickSheet = { comparisonViewModel.openQuickSheet() },
                                        onOpenFullComparison = { openComparison() },
                                        onClearAll = { comparisonViewModel.clearAll() }
                                    )
                                }
                                MarketZedBottomBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { tab ->
                                        if (tab == TopLevelTab.ACCOUNT) {
                                            accountDestination = AccountDestination.DASHBOARD
                                        }
                                        navigateTopLevel(tab)
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showTopLevelNav && useNavigationRail) {
                            MarketZedNavigationRail(
                                currentRoute = currentRoute,
                                onNavigate = { tab ->
                                    if (tab == TopLevelTab.ACCOUNT) {
                                        accountDestination = AccountDestination.DASHBOARD
                                    }
                                    navigateTopLevel(tab)
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            NavHost(
                                navController = navController,
                                startDestination = TopLevelTab.HOME.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(TopLevelTab.HOME.route) {
                                    HomeScreen(
                                        state = homeState,
                                        unreadNotifications = accountState.unreadNotificationCount,
                                        onRefresh = { homeViewModel.refresh(force = true) },
                                        onOpenSearch = { q -> openExploreWithFilter(categoryId = null, query = q) },
                                        onOpenCategory = { catId -> openExploreWithFilter(categoryId = catId) },
                                        onOpenExploreAll = { navigateTopLevel(TopLevelTab.EXPLORE) },
                                        onOpenListing = { id -> openListingDetail(id) },
                                        onToggleFavorite = { id -> homeViewModel.toggleFavorite(id) },
                                        onOpenPostWanted = {
                                            postViewModel.selectCategory("wanted")
                                            if (accountState.auth.isSignedIn) {
                                                navigateTopLevel(TopLevelTab.POST)
                                            } else {
                                                pendingReturnToPost = true
                                                accountDestination = AccountDestination.DASHBOARD
                                                navigateTopLevel(TopLevelTab.ACCOUNT)
                                            }
                                        },
                                        onOpenNotifications = {
                                            accountDestination = AccountDestination.NOTIFICATIONS
                                            navigateTopLevel(TopLevelTab.ACCOUNT)
                                        },
                                        onToggleDataSaver = { homeViewModel.toggleDataSaver() },
                                        onSubmitTestimonial = { name, town, msg, rating ->
                                            homeViewModel.submitTestimonial(name, town, msg, rating)
                                        },
                                        onClearTestimonialFeedback = { homeViewModel.clearTestimonialFeedback() },
                                        onRotateMotivationalMessage = { homeViewModel.rotateMotivationalMessage() },
                                        onClearSyncError = { homeViewModel.clearSyncError() },
                                        onOpenWantedTab = { navigateTopLevel(TopLevelTab.WANTED) },
                                        comparedIds = comparisonState.selectedIdSet,
                                        onToggleCompare = { listing -> comparisonViewModel.toggleCompare(listing) },
                                        onOpenSeller = { listing -> openSellerProfile(listing) },
                                        onClearRecentlyViewed = { homeViewModel.clearRecentlyViewed() },
                                        onOpenSubcategory = { catId, sub -> openExploreWithSubcategory(catId, sub) },
                                        onOpenLocationDiscovery = { town -> openExploreWithFilter(categoryId = null, query = town) },
                                        onOpenPostTab = { navigateTopLevel(TopLevelTab.POST) }
                                    )
                                }

                                composable(TopLevelTab.EXPLORE.route) {
                                    ExploreScreen(
                                        state = exploreState,
                                        onQueryChanged = { exploreViewModel.onQueryChanged(it) },
                                        onSubmitSearch = { exploreViewModel.submitSearch() },
                                        onSelectCategory = { exploreViewModel.selectCategory(it) },
                                        onSelectSubcategory = { exploreViewModel.selectSubcategory(it) },
                                        onSelectProvince = { exploreViewModel.selectProvince(it) },
                                        onSelectSort = { exploreViewModel.selectSort(it) },
                                        onMinPriceChanged = { exploreViewModel.updateMinPrice(it) },
                                        onMaxPriceChanged = { exploreViewModel.updateMaxPrice(it) },
                                        onClearFilters = { exploreViewModel.clearFilters() },
                                        onUseRecentSearch = { exploreViewModel.useRecentSearch(it) },
                                        onRemoveRecentSearch = { exploreViewModel.removeSearchHistoryItem(it) },
                                        onClearRecentSearches = { exploreViewModel.clearSearchHistory() },
                                        onLoadNextPage = { exploreViewModel.loadNextPage() },
                                        onOpenListing = { id -> openListingDetail(id) },
                                        onToggleFavorite = { id -> exploreViewModel.toggleFavorite(id) },
                                        wantedPreview = homeState.wantedPreview.firstOrNull(),
                                        latestJobPreview = (homeState.promotedJobs + homeState.latestJobs).firstOrNull(),
                                        servicePreview = homeState.servicesPreview.firstOrNull(),
                                        onOpenWantedTab = { navigateTopLevel(TopLevelTab.WANTED) },
                                        onOpenAssistant = null,
                                        onClearBudgetFilter = { exploreViewModel.clearBudgetFilter() },
                                        comparedIds = comparisonState.selectedIdSet,
                                        onToggleCompare = { listing -> comparisonViewModel.toggleCompare(listing) },
                                        onOpenSeller = { listing -> openSellerProfile(listing) },
                                        onSelectDistrict = { exploreViewModel.selectDistrict(it) },
                                        onSelectTown = { exploreViewModel.selectTown(it) },
                                        onNearbyAreaChanged = { exploreViewModel.onNearbyAreaChanged(it) },
                                        onSelectCondition = { exploreViewModel.selectCondition(it) },
                                        onSaveSearch = { name, notify -> exploreViewModel.saveCurrentSearch(name, notify) },
                                        onApplySavedSearch = { exploreViewModel.applySavedSearch(it) },
                                        onToggleSavedSearchAlerts = { id, enabled -> exploreViewModel.toggleSavedSearchAlerts(id, enabled) },
                                        onDeleteSavedSearch = { exploreViewModel.deleteSavedSearch(it) },
                                        onClearSaveSearchFeedback = { exploreViewModel.clearSaveSearchFeedback() }
                                    )
                                }

                                composable(TopLevelTab.POST.route) {
                                    PostListingScreen(
                                        state = postState,
                                        onRequireSignIn = {
                                            pendingReturnToPost = true
                                            accountDestination = AccountDestination.DASHBOARD
                                            navigateTopLevel(TopLevelTab.ACCOUNT)
                                        },
                                        onOpenActivation = {
                                            accountDestination = AccountDestination.ACTIVATION
                                            navigateTopLevel(TopLevelTab.ACCOUNT)
                                        },
                                        onRefreshProfile = { postViewModel.refreshProfileAccess() },
                                        onSelectCategory = { postViewModel.selectCategory(it) },
                                        onUpdateDraft = { postViewModel.updateDraft(it) },
                                        onSetStep = { postViewModel.setStep(it) },
                                        onPickImages = { uris -> postViewModel.addPickedImages(uris) },
                                        onRemovePhoto = { path -> postViewModel.removePhoto(path) },
                                        onRetryPhotoUpload = { path -> postViewModel.retryPhotoUpload(path) },
                                        onSaveDraftNow = { postViewModel.saveDraftNow() },
                                        onClearDraftNotice = { postViewModel.clearDraftSavedNotice() },
                                        onLoadDraft = { draft -> postViewModel.loadSavedDraft(draft) },
                                        onDeleteDraft = { uuid -> postViewModel.deleteSavedDraft(uuid) },
                                        onStartNewDraft = { postViewModel.startNewDraft() },
                                        onSubmitListing = { postViewModel.submitListing() },
                                        onViewPublishedListing = { id ->
                                            postViewModel.startNewDraft()
                                            openListingDetail(id)
                                        },
                                        onOpenMyListings = {
                                            postViewModel.startNewDraft()
                                            accountDestination = AccountDestination.MY_LISTINGS
                                            navigateTopLevel(TopLevelTab.ACCOUNT)
                                        },
                                        onReturnHome = { navigateTopLevel(TopLevelTab.HOME) },
                                        onAcknowledgeWantedGate = { postViewModel.acknowledgeWantedGate() }
                                    )
                                }

                                composable(TopLevelTab.WANTED.route) {
                                    WantedScreen(
                                        state = wantedState,
                                        onQueryChanged = { wantedViewModel.onQueryChanged(it) },
                                        onSelectProvince = { wantedViewModel.selectProvince(it) },
                                        onSelectSort = { wantedViewModel.selectSort(it) },
                                        onClearFilters = { wantedViewModel.clearFilters() },
                                        onRefresh = { wantedViewModel.refresh(force = true) },
                                        onLoadNextPage = { wantedViewModel.loadNextPage() },
                                        onOpenListing = { id -> openListingDetail(id) },
                                        onToggleFavorite = { id -> wantedViewModel.toggleFavorite(id) },
                                        onPostWanted = {
                                            postViewModel.selectCategory("wanted")
                                            if (accountState.auth.isSignedIn) {
                                                navigateTopLevel(TopLevelTab.POST)
                                            } else {
                                                pendingReturnToPost = true
                                                accountDestination = AccountDestination.DASHBOARD
                                                navigateTopLevel(TopLevelTab.ACCOUNT)
                                            }
                                        },
                                        onClearError = { wantedViewModel.clearError() },
                                        onOpenSeller = { listing -> openSellerProfile(listing) }
                                    )
                                }

                                composable(TopLevelTab.ACCOUNT.route) {
                                    if (accountDestination != AccountDestination.DASHBOARD) {
                                        BackHandler {
                                            accountDestination = AccountDestination.DASHBOARD
                                        }
                                    }
                                    when (accountDestination) {
                                        AccountDestination.DASHBOARD -> AccountScreen(
                                            state = accountState,
                                            onSignIn = { email, pwd -> accountViewModel.signIn(email, pwd) },
                                            onRegister = { name, email, pwd, conf ->
                                                accountViewModel.register(name, email, pwd, conf)
                                            },
                                            onPasswordReset = { email -> accountViewModel.sendPasswordReset(email) },
                                            onResendVerification = { accountViewModel.resendVerificationEmail() },
                                            onRefreshVerification = { accountViewModel.refreshEmailVerificationStatus() },
                                            onSignOut = {
                                                pendingReturnToPost = false
                                                adminViewModel.clearState()
                                                accountViewModel.signOut()
                                            },
                                            onNavigateSubScreen = { dest -> accountDestination = dest },
                                            onClearAuthBanners = { accountViewModel.clearAuthBanners() },
                                            onClearSessionExpiredPrompt = { accountViewModel.clearSessionExpiredPrompt() }
                                        )
                                        AccountDestination.ADMIN_DASHBOARD -> AdminDashboardScreen(
                                            isAdmin = accountState.isAdmin,
                                            adminEmail = accountState.auth.email ?: accountState.profile?.email.orEmpty(),
                                            adminRoleLabel = accountState.profile?.role ?: accountState.auth.role,
                                            state = adminState,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onLoadInitial = { adminViewModel.loadAllInitial() },
                                            onSelectTab = { adminViewModel.selectTab(it) },
                                            onRefreshTab = { adminViewModel.refreshCurrentTab() },
                                            onSetListingFilter = { adminViewModel.setListingFilter(it) },
                                            onSetListingQuery = { adminViewModel.setListingQuery(it) },
                                            onSearchListings = { adminViewModel.loadListings() },
                                            onModerateListing = { id, action, reason ->
                                                adminViewModel.moderateListing(id, action, reason)
                                            },
                                            onOpenListing = { id -> openListingDetail(id) },
                                            onSetPromotionFilter = { adminViewModel.setPromotionFilter(it) },
                                            onManagePromotion = { promoId, action, days, reason, format ->
                                                adminViewModel.managePromotion(promoId, action, days, reason, format)
                                            },
                                            onGrantPromotion = { listingId, days, note, format ->
                                                adminViewModel.grantPromotion(listingId, days, note, format)
                                            },
                                            onUpdatePromotionPricing = { p7, p14, p30 ->
                                                adminViewModel.updatePromotionPricing(p7, p14, p30)
                                            },
                                            onSetReportFilter = { adminViewModel.setReportFilter(it) },
                                            onResolveReport = { id, action, note ->
                                                adminViewModel.resolveReport(id, action, note)
                                            },
                                            onSetUserFilter = { adminViewModel.setUserFilter(it) },
                                            onSetUserQuery = { adminViewModel.setUserQuery(it) },
                                            onSearchUsers = { adminViewModel.loadUsers() },
                                            onManageUser = { uid, action, reason ->
                                                adminViewModel.manageUser(uid, action, reason)
                                            },
                                            onSetPaymentFilter = { adminViewModel.setPaymentFilter(it) },
                                            onSendBroadcast = { title, message, catId, listingId ->
                                                adminViewModel.sendBroadcast(title, message, catId, listingId)
                                            },
                                            onClearFeedback = { adminViewModel.clearFeedback() }
                                        )
                                        AccountDestination.PROFILE_EDIT -> ProfileEditScreen(
                                            state = accountState,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onSaveProfile = { first, last, contact, dob, wa, alt, type, prov, town ->
                                                accountViewModel.saveProfile(first, last, contact, dob, wa, alt, type, prov, town)
                                            },
                                            onClearMessages = { accountViewModel.clearProfileMessages() }
                                        )
                                        AccountDestination.ACTIVATION -> ActivationPaymentScreen(
                                            state = accountState,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onStartPayment = { phone, payerType ->
                                                accountViewModel.startK30ActivationPayment(phone, payerType)
                                            },
                                            onCheckStatus = { accountViewModel.checkActivePaymentStatus() },
                                            onClearPaymentState = { accountViewModel.clearActivePaymentState() }
                                        )
                                        AccountDestination.MY_LISTINGS -> MyListingsScreen(
                                            state = accountState,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onLoadMyListings = { accountViewModel.loadMyListings() },
                                            onOpenListing = { id -> openListingDetail(id) },
                                            onManageListing = { id, action -> accountViewModel.manageListing(id, action) },
                                            onReviseListing = { id, changes, cb ->
                                                accountViewModel.reviseListing(id, changes, cb)
                                            },
                                            onUploadEditImage = { uri -> accountViewModel.uploadImageFromUriForEdit(uri) },
                                            onPromoteListing = { listing ->
                                                preselectedPromoListing = listing
                                                accountDestination = AccountDestination.PROMOTIONS
                                            },
                                            onPostListing = { navigateTopLevel(TopLevelTab.POST) },
                                            onClearActionMessage = { accountViewModel.clearListingActionMessage() },
                                            onClearListingsError = { accountViewModel.clearMyListingsError() },
                                            onRequireSignInAgain = {
                                                accountViewModel.signOut()
                                                accountDestination = AccountDestination.DASHBOARD
                                            },
                                            onClearSessionExpiredPrompt = { accountViewModel.clearSessionExpiredPrompt() }
                                        )
                                        AccountDestination.PROMOTIONS -> PromotionsScreen(
                                            state = accountState,
                                            preselectedListing = preselectedPromoListing,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onLoadPromotions = {
                                                accountViewModel.loadMyListings()
                                                accountViewModel.loadPromotionsAndPricing()
                                            },
                                            onStartPromotionPayment = { listingId, days, phone, payerType, format ->
                                                accountViewModel.startPromotionPayment(listingId, days, phone, payerType, format)
                                            },
                                            onCheckStatus = { accountViewModel.checkActivePaymentStatus() },
                                            onClearPaymentState = { accountViewModel.clearActivePaymentState() },
                                            onClearPromotionsError = { accountViewModel.clearPromotionsError() },
                                            onOpenMyListings = { accountDestination = AccountDestination.MY_LISTINGS },
                                            onOpenPostListing = { navigateTopLevel(TopLevelTab.POST) }
                                        )
                                        AccountDestination.PAYMENT_HISTORY -> PaymentHistoryScreen(
                                            state = accountState,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onLoadPayments = { accountViewModel.loadPayments() },
                                            onClearPaymentsError = { accountViewModel.clearPaymentsError() },
                                            onOpenVerification = { accountDestination = AccountDestination.ACTIVATION },
                                            onOpenPromotions = { accountDestination = AccountDestination.PROMOTIONS }
                                        )
                                        AccountDestination.SAVED -> {
                                            BackHandler { accountDestination = AccountDestination.DASHBOARD }
                                            SecondaryPageScaffold(
                                                title = "Saved & History",
                                                onBack = { accountDestination = AccountDestination.DASHBOARD },
                                                backButtonTestTag = "saved_history_back_button"
                                            ) { padding ->
                                                SavedScreen(
                                                    state = savedState,
                                                    onOpenListing = { id -> openListingDetail(id) },
                                                    onToggleFavorite = { id -> savedViewModel.toggleFavorite(id) },
                                                    onClearRecentlyViewed = { savedViewModel.clearRecentlyViewed() },
                                                    onExploreClick = { navigateTopLevel(TopLevelTab.EXPLORE) },
                                                    comparedIds = comparisonState.selectedIdSet,
                                                    onToggleCompare = { listing -> comparisonViewModel.toggleCompare(listing) },
                                                    onOpenSeller = { listing -> openSellerProfile(listing) },
                                                    showHeader = false,
                                                    modifier = Modifier.padding(padding)
                                                )
                                            }
                                        }
                                        AccountDestination.NOTIFICATIONS -> NotificationsScreen(
                                            state = accountState,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onRefresh = { accountViewModel.loadNotifications() },
                                            onMarkRead = { id -> accountViewModel.markNotificationRead(id) },
                                            onMarkAllRead = { accountViewModel.markAllNotificationsRead() },
                                            onDeleteNotification = { id -> accountViewModel.deleteNotification(id) },
                                            onMarkAnnouncementRead = { id -> accountViewModel.markAnnouncementRead(id) },
                                            onOpenListing = { id -> openListingDetail(id) },
                                            onOpenCategory = { catId -> openExploreWithFilter(catId) },
                                            onOpenVerification = { accountDestination = AccountDestination.ACTIVATION },
                                            onOpenPayments = { accountDestination = AccountDestination.PAYMENT_HISTORY },
                                            onOpenPromotions = { accountDestination = AccountDestination.PROMOTIONS },
                                            onOpenWanted = { navigateTopLevel(TopLevelTab.WANTED) },
                                            onBrowseMarketplace = { navigateTopLevel(TopLevelTab.EXPLORE) },
                                            onSetGeneralNotifications = { accountViewModel.setGeneralNotificationsEnabled(it) },
                                            onSetPromotionalNotifications = { accountViewModel.setPromotionalNotificationsEnabled(it) },
                                            onSetSmartEngagement = { accountViewModel.setSmartEngagementEnabled(it) },
                                            onSetMotivationalNotifications = { accountViewModel.setMotivationalNotificationsEnabled(it) },
                                            onClearNotificationsError = { accountViewModel.clearNotificationsError() },
                                            onSetQuietHours = { accountViewModel.setQuietHoursEnabled(it) }
                                        )
                                        AccountDestination.SETTINGS -> SettingsAndDiagnosticsScreen(
                                            state = accountState,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD },
                                            onSetThemeMode = { accountViewModel.setThemeMode(it) },
                                            onSetDataSaver = { accountViewModel.setDataSaver(it) },
                                            onSetWifiOnlyPrefetch = { accountViewModel.setWifiOnlyPrefetch(it) },
                                            onSetGeneralNotifications = { accountViewModel.setGeneralNotificationsEnabled(it) },
                                            onSetPromotionalNotifications = { accountViewModel.setPromotionalNotificationsEnabled(it) },
                                            onSetSmartEngagement = { accountViewModel.setSmartEngagementEnabled(it) },
                                            onSetMotivationalNotifications = { accountViewModel.setMotivationalNotificationsEnabled(it) },
                                            onTriggerBackgroundSync = { accountViewModel.triggerBackgroundSync() },
                                            onClearPublicCache = { accountViewModel.clearPublicCacheOnly() },
                                            onClearCacheMessage = { accountViewModel.clearCacheMessage() },
                                            onSetQuietHours = { accountViewModel.setQuietHoursEnabled(it) },
                                            onSeedDevFixtures = { accountViewModel.seedDevFixtures() },
                                            onClearDevFixtures = { accountViewModel.clearDevFixtures() }
                                        )
                                        AccountDestination.ABOUT,
                                        AccountDestination.SAFETY,
                                        AccountDestination.PRIVACY,
                                        AccountDestination.TERMS -> StaticInfoScreen(
                                            destination = accountDestination,
                                            onBack = { accountDestination = AccountDestination.DASHBOARD }
                                        )
                                    }
                                }

                                composable(
                                    route = ROUTE_DETAIL,
                                    arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val rawId = backStackEntry.arguments?.getString("listingId").orEmpty()
                                    val listingId = remember(rawId) { Uri.decode(rawId) }
                                    val detailViewModel: ListingDetailViewModel = viewModel(factory = factory)
                                    LaunchedEffect(listingId) {
                                        if (listingId.isNotBlank()) {
                                            detailViewModel.loadListing(listingId)
                                        }
                                    }
                                    val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()
                                    ListingDetailScreen(
                                        state = detailState,
                                        onBack = { navController.popBackStack() },
                                        onToggleFavorite = { detailViewModel.toggleFavorite() },
                                        onRequireSignInForReport = {
                                            accountDestination = AccountDestination.DASHBOARD
                                            navigateTopLevel(TopLevelTab.ACCOUNT)
                                        },
                                        onSubmitReport = { reason, details ->
                                            detailViewModel.submitReport(reason, details)
                                        },
                                        onClearReportFeedback = { detailViewModel.clearReportFeedback() },
                                        isCompared = detailState.listing?.id in comparisonState.selectedIdSet,
                                        onToggleCompare = {
                                            detailState.listing?.let { comparisonViewModel.toggleCompare(it) }
                                        },
                                        onOpenListing = { similarId -> openListingDetail(similarId) },
                                        onOpenSeller = { sellerListing -> openSellerProfile(sellerListing) },
                                        onToggleSimilarFavorite = { similarId ->
                                            homeViewModel.toggleFavorite(similarId)
                                        }
                                    )
                                }

                                composable(ROUTE_COMPARE) {
                                    ComparisonScreen(
                                        state = comparisonState,
                                        dataSaverEnabled = accountState.settings.dataSaverEnabled,
                                        shareSummary = comparisonViewModel.buildShareSummary(),
                                        onBack = { navController.popBackStack() },
                                        onOpenListing = { id -> openListingDetail(id) },
                                        onOpenSeller = { listing -> openSellerProfile(listing) },
                                        onRemoveItem = { id -> comparisonViewModel.removeItem(id) },
                                        onClearAll = { comparisonViewModel.clearAll() },
                                        onToggleShowDifferencesOnly = { comparisonViewModel.setShowDifferencesOnly(it) },
                                        onBrowseProducts = {
                                            navController.popBackStack()
                                            openExploreWithFilter("marketplace")
                                        }
                                    )
                                }

                                composable(
                                    route = ROUTE_SELLER,
                                    arguments = listOf(
                                        navArgument("sellerKey") { type = NavType.StringType },
                                        navArgument("listingId") {
                                            type = NavType.StringType
                                            nullable = true
                                            defaultValue = null
                                        }
                                    )
                                ) { backStackEntry ->
                                    val rawSellerKey = backStackEntry.arguments?.getString("sellerKey").orEmpty()
                                    val rawListingId = backStackEntry.arguments?.getString("listingId")
                                    val decodedSellerKey = remember(rawSellerKey) { Uri.decode(rawSellerKey) }
                                    val decodedListingId = remember(rawListingId) { rawListingId?.let(Uri::decode) }
                                    val sellerViewModel: SellerProfileViewModel = viewModel(factory = factory)
                                    LaunchedEffect(decodedSellerKey, decodedListingId) {
                                        if (decodedSellerKey.isNotBlank()) {
                                            sellerViewModel.loadSeller(decodedSellerKey, decodedListingId)
                                        }
                                    }
                                    val sellerState by sellerViewModel.uiState.collectAsStateWithLifecycle()
                                    SellerProfileScreen(
                                        state = sellerState,
                                        comparedIds = comparisonState.selectedIdSet,
                                        onBack = { navController.popBackStack() },
                                        onSelectCategory = { sellerViewModel.selectCategory(it) },
                                        onOpenListing = { id -> openListingDetail(id) },
                                        onToggleFavorite = { id -> sellerViewModel.toggleFavorite(id) },
                                        onToggleCompare = { listing -> comparisonViewModel.toggleCompare(listing) }
                                    )
                                }
                            }

                            if (showTopLevelNav && useNavigationRail && comparisonState.count > 0 && !imeVisible) {
                                ComparisonFloatingBar(
                                    state = comparisonState,
                                    onOpenQuickSheet = { comparisonViewModel.openQuickSheet() },
                                    onOpenFullComparison = { openComparison() },
                                    onClearAll = { comparisonViewModel.clearAll() },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }

                TopSnackbarHost(
                    controller = topSnackbarController,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        if (comparisonState.isSheetOpen) {
            QuickComparisonSheet(
                state = comparisonState,
                dataSaverEnabled = accountState.settings.dataSaverEnabled,
                onDismiss = { comparisonViewModel.dismissQuickSheet() },
                onOpenListing = { id ->
                    comparisonViewModel.dismissQuickSheet()
                    openListingDetail(id)
                },
                onRemoveItem = { id -> comparisonViewModel.removeItem(id) },
                onAddProductClick = {
                    comparisonViewModel.dismissQuickSheet()
                    openExploreWithFilter("marketplace")
                },
                onExpandComparison = {
                    comparisonViewModel.dismissQuickSheet()
                    openComparison()
                },
                onClearAll = { comparisonViewModel.clearAll() }
            )
        }

        if (comparisonState.showLimitAlert) {
            AlertDialog(
                onDismissRequest = { comparisonViewModel.dismissFeedback() },
                title = { Text("Maximum 3 items") },
                text = { Text("Remove one item from your comparison tray before adding another.") },
                confirmButton = {
                    Button(
                        onClick = {
                            comparisonViewModel.dismissFeedback()
                            comparisonViewModel.openQuickSheet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                    ) {
                        Text("Review Selection")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { comparisonViewModel.dismissFeedback() }) {
                        Text("OK")
                    }
                }
            )
        }

        activeAnnouncementDialog?.let { ann ->
            AlertDialog(
                onDismissRequest = { activeAnnouncementDialog = null },
                title = { Text(ann.title) },
                text = { Text(ann.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { activeAnnouncementDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun MarketZedNavigationRail(
    currentRoute: String?,
    onNavigate: (TopLevelTab) -> Unit
) {
    NavigationRail(
        header = {
            FloatingActionButton(
                onClick = { onNavigate(TopLevelTab.POST) },
                containerColor = MarketZedOrange,
                contentColor = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(painter = painterResource(LucideR.drawable.lucide_ic_plus), contentDescription = "Post listing")
            }
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    ) {
        val railItems = listOf(
            TopLevelTab.HOME to LucideR.drawable.lucide_ic_house,
            TopLevelTab.EXPLORE to LucideR.drawable.lucide_ic_compass,
            TopLevelTab.WANTED to LucideR.drawable.lucide_ic_handshake,
            TopLevelTab.ACCOUNT to LucideR.drawable.lucide_ic_user
        )
        railItems.forEach { (tab, iconRes) ->
            val selected = currentRoute == tab.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(tab) },
                icon = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MarketZedOrange,
                    selectedTextColor = MarketZedOrange,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
