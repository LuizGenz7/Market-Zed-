package com.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.HomepageEmptyMessages
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.dev.DevFixturesProviderFactory
import com.example.data.model.AccountVerificationUiStatus
import com.example.data.model.AppNotification
import com.example.data.model.CategoryDetails
import com.example.data.model.ContactPreferences
import com.example.data.model.Listing
import com.example.data.model.ListingDraft
import com.example.data.model.ListingLocation
import com.example.data.model.PromotionPricing
import com.example.data.model.UserProfile
import com.example.data.repository.InMemoryLocalAccountCache
import com.example.ui.components.PaymentUiStatePhase
import com.example.ui.components.VerticalContactActionsColumn
import com.example.ui.components.contextSensitiveContactLabel
import com.example.ui.screens.account.AccountScreen
import com.example.ui.screens.account.AccountUiState
import com.example.ui.screens.account.ListingEditValidation
import com.example.ui.screens.account.NotificationSemantics
import com.example.ui.screens.account.NotificationsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeUiState
import com.example.ui.screens.post.PostListingScreen
import com.example.ui.screens.post.PostListingUiState
import com.example.ui.theme.MarketZedTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MarketZedEnhancementsSpecTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `notification semantics map categories to expected Lucide icons and badges`() {
        val wantedStyle = NotificationSemantics.resolveStyle("wanted_match", "New Wanted match")
        assertEquals(LucideR.drawable.lucide_ic_handshake, wantedStyle.iconRes)
        assertEquals("Wanted", wantedStyle.categoryBadge)

        val paymentStyle = NotificationSemantics.resolveStyle("payment_confirmed", "Lipila Receipt")
        assertEquals(LucideR.drawable.lucide_ic_wallet, paymentStyle.iconRes)
        assertEquals("Payment", paymentStyle.categoryBadge)

        val verificationStyle = NotificationSemantics.resolveStyle("account_verified", "Verification Approved")
        assertEquals(LucideR.drawable.lucide_ic_shield_check, verificationStyle.iconRes)
        assertEquals("Verification", verificationStyle.categoryBadge)

        val promoStyle = NotificationSemantics.resolveStyle("promotion_active", "Boost live")
        assertEquals(LucideR.drawable.lucide_ic_megaphone, promoStyle.iconRes)
        assertEquals("Promotion", promoStyle.categoryBadge)

        val listingStyle = NotificationSemantics.resolveStyle("listing_published", "Item Live")
        assertEquals(LucideR.drawable.lucide_ic_package, listingStyle.iconRes)
        assertEquals("Listing", listingStyle.categoryBadge)

        val securityStyle = NotificationSemantics.resolveStyle("security_alert", "Password changed")
        assertEquals(LucideR.drawable.lucide_ic_shield, securityStyle.iconRes)
        assertEquals("Security", securityStyle.categoryBadge)

        val generalStyle = NotificationSemantics.resolveStyle("general", "Welcome")
        assertEquals(LucideR.drawable.lucide_ic_bell, generalStyle.iconRes)
        assertEquals("General", generalStyle.categoryBadge)
    }

    @Test
    fun `notifications screen renders structured cards mark all read delete and clean empty state`() {
        val notif = AppNotification(
            id = "n_1",
            type = "wanted_match",
            title = "Wanted Request Match",
            message = "Someone posted an item matching your Laptop request.",
            read = false,
            categoryId = "wanted"
        )
        var markAllClicked = false
        var deletedId: String? = null

        composeRule.setContent {
            MarketZedTheme {
                NotificationsScreen(
                    state = AccountUiState(notifications = listOf(notif)),
                    onBack = {},
                    onRefresh = {},
                    onMarkRead = {},
                    onMarkAllRead = { markAllClicked = true },
                    onDeleteNotification = { deletedId = it },
                    onMarkAnnouncementRead = {},
                    onOpenListing = {},
                    onOpenCategory = {},
                    onOpenVerification = {},
                    onOpenPayments = {},
                    onOpenPromotions = {},
                    onOpenWanted = {},
                    onBrowseMarketplace = {},
                    onSetGeneralNotifications = {},
                    onSetPromotionalNotifications = {},
                    onSetSmartEngagement = {},
                    onSetMotivationalNotifications = {},
                    onClearNotificationsError = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_card_n_1").assertIsDisplayed()
        composeRule.onNodeWithTag("notifications_mark_all_read").performClick()
        assertTrue(markAllClicked)

        composeRule.onNodeWithTag("notification_delete_n_1").performClick()
        assertEquals("n_1", deletedId)
    }

    @Test
    fun `notifications screen shows crisp empty state with zero fake items`() {
        composeRule.setContent {
            MarketZedTheme {
                NotificationsScreen(
                    state = AccountUiState(notifications = emptyList(), localAnnouncements = emptyList()),
                    onBack = {},
                    onRefresh = {},
                    onMarkRead = {},
                    onMarkAllRead = {},
                    onDeleteNotification = {},
                    onMarkAnnouncementRead = {},
                    onOpenListing = {},
                    onOpenCategory = {},
                    onOpenVerification = {},
                    onOpenPayments = {},
                    onOpenPromotions = {},
                    onOpenWanted = {},
                    onBrowseMarketplace = {},
                    onSetGeneralNotifications = {},
                    onSetPromotionalNotifications = {},
                    onSetSmartEngagement = {},
                    onSetMotivationalNotifications = {},
                    onClearNotificationsError = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No notifications yet").assertIsDisplayed()
    }

    @Test
    fun `listing edit validation enforces rental-only property policy and Wanted zero images`() {
        val saleError = ListingEditValidation.validate(
            categoryId = "property",
            subcategory = "Houses",
            title = "4 Bedroom House for Sale in Roma",
            description = "Lovely brick home for sale with electric fence and paved driveway.",
            priceInput = "2500000",
            province = "Lusaka",
            town = "Lusaka",
            phone = "0971234567",
            whatsapp = "",
            email = ""
        )
        assertTrue(saleError.containsKey("policy"))

        val validRental = ListingEditValidation.validate(
            categoryId = "property",
            subcategory = "Houses",
            title = "3 Bedroom House for Rent in Woodlands",
            description = "Clean 3 bedroom house for rent with borehole water and carport.",
            priceInput = "9500",
            province = "Lusaka",
            town = "Lusaka",
            phone = "0971234567",
            whatsapp = "",
            email = ""
        )
        assertTrue(validRental.isEmpty())

        val validWanted = ListingEditValidation.validate(
            categoryId = "wanted",
            subcategory = "Products wanted",
            title = "Looking for ThinkPad T480",
            description = "Need a clean Core i5 16GB RAM laptop in Lusaka this week.",
            priceInput = "6800",
            province = "Lusaka",
            town = "Lusaka",
            phone = "0971234567",
            whatsapp = "",
            email = ""
        )
        assertTrue(validWanted.isEmpty())
    }

    @Test
    fun `payment phase resolution and verification fee fallback behave accurately`() {
        val pricing = PromotionPricing(prices = mapOf("7" to 25.0, "account_verification" to 35.0))
        assertEquals(35.0, pricing.verificationFeeOrDefault(), 0.01)

        val defaultFeeState = AccountUiState()
        assertEquals(30.0, defaultFeeState.verificationFeeZmw, 0.01)

        val processingState = AccountUiState(paymentBusy = true, activePaymentMessage = "Confirming...")
        assertTrue(
            processingState.resolvePaymentPhase("Verify", 30.0, "Active") is PaymentUiStatePhase.Processing
        )

        val successState = AccountUiState(
            activePaymentId = "pay_99",
            activePaymentStatus = "successful",
            activePaymentAmountZmw = 30.0
        )
        assertTrue(
            successState.resolvePaymentPhase("Verify", 30.0, "Active") is PaymentUiStatePhase.Successful
        )

        val pendingState = AccountUiState(
            activePaymentId = "pay_99",
            activePaymentStatus = "pending"
        )
        assertTrue(
            pendingState.resolvePaymentPhase("Verify", 30.0, "Active") is PaymentUiStatePhase.Pending
        )

        val cancelledState = AccountUiState(
            activePaymentId = "pay_99",
            activePaymentStatus = "cancelled"
        )
        assertTrue(
            cancelledState.resolvePaymentPhase("Verify", 30.0, "Active") is PaymentUiStatePhase.Cancelled
        )

        val failedState = AccountUiState(
            activePaymentId = "pay_99",
            activePaymentStatus = "failed"
        )
        assertTrue(
            failedState.resolvePaymentPhase("Verify", 30.0, "Active") is PaymentUiStatePhase.Failed
        )
    }

    @Test
    fun `account screen displays prominent Verify Account CTA reflecting all four verification states`() {
        val unverified = UserProfile(uid = "u1", email = "a@b.com", firstName = "Chanda", verified = false)
        assertEquals(AccountVerificationUiStatus.NOT_VERIFIED, unverified.resolvedVerificationStatus)

        val pending = unverified.copy(verificationStatus = "pending")
        assertEquals(AccountVerificationUiStatus.PENDING, pending.resolvedVerificationStatus)

        val failed = unverified.copy(verificationStatus = "failed")
        assertEquals(AccountVerificationUiStatus.FAILED, failed.resolvedVerificationStatus)

        val verified = unverified.copy(verified = true, verificationStatus = "verified")
        assertEquals(AccountVerificationUiStatus.VERIFIED, verified.resolvedVerificationStatus)

        composeRule.setContent {
            MarketZedTheme {
                AccountScreen(
                    state = AccountUiState(
                        auth = com.example.data.auth.AuthUserState(
                            isSignedIn = true,
                            uid = "u1",
                            email = "a@b.com",
                            displayName = "Chanda Mwamba",
                            isEmailVerified = true
                        ),
                        profile = unverified
                    ),
                    onSignIn = { _, _ -> },
                    onRegister = { _, _, _, _ -> },
                    onPasswordReset = {},
                    onResendVerification = {},
                    onRefreshVerification = {},
                    onSignOut = {},
                    onNavigateSubScreen = {},
                    onClearAuthBanners = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_verify_cta").assertIsDisplayed()
        composeRule.onNodeWithText("Verify Account").assertIsDisplayed()
    }

    @Test
    fun `location hierarchy supports Province District Town NearbyArea without coordinates`() {
        val lusakaDistricts = MarketZedCatalogue.districtsForProvince("Lusaka")
        assertTrue(lusakaDistricts.contains("Lusaka District"))
        assertTrue(lusakaDistricts.contains("Chilanga District"))

        val location = ListingLocation(
            province = "Lusaka",
            district = "Lusaka District",
            town = "Lusaka",
            nearbyArea = "Woodlands"
        )
        assertEquals("Lusaka", location.displayTown)
        assertEquals("Woodlands, Lusaka, Lusaka District", location.displayFull)
    }

    @Test
    fun `homepage empty states provide all 8 approved messages and hide empty sections`() {
        assertEquals(8, HomepageEmptyMessages.messages.size)
        val picked = HomepageEmptyMessages.pickStableMessage(index = 2)
        assertEquals(HomepageEmptyMessages.messages[2], picked)

        val emptyHomeState = HomeUiState(hasLoadedLocalCache = true)
        assertFalse(emptyHomeState.hasAnyMarketplaceContent)

        composeRule.setContent {
            MarketZedTheme {
                HomeScreen(
                    state = emptyHomeState.copy(emptyStateMessage = picked),
                    unreadNotifications = 0,
                    onRefresh = {},
                    onOpenSearch = {},
                    onOpenCategory = {},
                    onOpenExploreAll = {},
                    onOpenListing = {},
                    onToggleFavorite = {},
                    onOpenPostWanted = {},
                    onOpenNotifications = {},
                    onToggleDataSaver = {},
                    onSubmitTestimonial = { _, _, _, _ -> },
                    onClearTestimonialFeedback = {},
                    onRotateMotivationalMessage = {},
                    onClearSyncError = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("YOUR MARKETPLACE STARTS HERE").assertIsDisplayed()
        composeRule.onNodeWithText(picked).assertIsDisplayed()
        composeRule.onNodeWithTag("home_empty_post_first_button").assertIsDisplayed()
        // Empty category sections must NOT show
        composeRule.onAllNodesWithText("Recommended Products").assertCountEquals(0)
        composeRule.onAllNodesWithText("Rental Properties").assertCountEquals(0)
        // Category shortcuts must still show
        composeRule.onNodeWithTag("category_tile_marketplace").assertIsDisplayed()
    }

    @Test
    fun `wanted 1-free-post then account verification gate works for unverified and verified users`() {
        val unverifiedFirstTime = PostListingUiState(
            isSignedIn = true,
            profile = UserProfile(uid = "u1", verified = false, freeWantedPostUsed = false, wantedPostsCount = 0),
            draft = ListingDraft(localUuid = "d1", userScope = "u1", categoryId = "wanted"),
            localFreeWantedUsed = false
        )
        assertTrue(unverifiedFirstTime.isFirstFreeWantedEligible)
        assertFalse(unverifiedFirstTime.needsVerificationForNextWanted)
        assertTrue(unverifiedFirstTime.isEligibleToPublishSelectedCategory)

        val unverifiedSecondTime = unverifiedFirstTime.copy(
            profile = UserProfile(uid = "u1", verified = false, freeWantedPostUsed = true, wantedPostsCount = 1),
            localFreeWantedUsed = true
        )
        assertFalse(unverifiedSecondTime.isFirstFreeWantedEligible)
        assertTrue(unverifiedSecondTime.needsVerificationForNextWanted)
        assertFalse(unverifiedSecondTime.isEligibleToPublishSelectedCategory)

        val verifiedAnyTime = unverifiedSecondTime.copy(
            profile = UserProfile(uid = "u1", verified = true, freeWantedPostUsed = true, wantedPostsCount = 5)
        )
        assertFalse(verifiedAnyTime.needsVerificationForNextWanted)
        assertTrue(verifiedAnyTime.isEligibleToPublishSelectedCategory)

        composeRule.setContent {
            MarketZedTheme {
                PostListingScreen(
                    state = unverifiedSecondTime,
                    onRequireSignIn = {},
                    onOpenActivation = {},
                    onRefreshProfile = {},
                    onSelectCategory = {},
                    onUpdateDraft = {},
                    onSetStep = {},
                    onPickImages = {},
                    onRemovePhoto = {},
                    onRetryPhotoUpload = {},
                    onSaveDraftNow = {},
                    onClearDraftNotice = {},
                    onLoadDraft = {},
                    onDeleteDraft = {},
                    onStartNewDraft = {},
                    onSubmitListing = {},
                    onViewPublishedListing = {},
                    onOpenMyListings = {},
                    onReturnHome = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("post_wanted_unlock_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("post_wanted_verify_button").assertIsDisplayed()
    }

    @Test
    fun `vertical contact actions column renders context-sensitive labels and hides missing methods`() {
        assertEquals("Contact Seller", contextSensitiveContactLabel("marketplace"))
        assertEquals("Contact Landlord", contextSensitiveContactLabel("property", "individual"))
        assertEquals("Contact Agent", contextSensitiveContactLabel("property", "business"))
        assertEquals("Contact Provider", contextSensitiveContactLabel("services"))
        assertEquals("Contact Employer", contextSensitiveContactLabel("jobs"))
        assertEquals("Contact Requester", contextSensitiveContactLabel("wanted"))

        composeRule.setContent {
            MarketZedTheme {
                VerticalContactActionsColumn(
                    contactPreferences = ContactPreferences(
                        phone = "260971234567",
                        whatsapp = null,
                        email = "owner@marketzed.zm"
                    ),
                    listingTitle = "2 Bedroom Flat in Woodlands",
                    categoryId = "property",
                    sellerAccountType = "individual"
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vertical_contact_actions_column").assertIsDisplayed()
        composeRule.onNodeWithText("Contact Landlord").assertIsDisplayed()
        composeRule.onNodeWithText("Send Message").assertIsDisplayed()
        composeRule.onAllNodesWithText("WhatsApp").assertCountEquals(0)
    }

    @Test
    fun `dev fixtures provider generates exact 120 Zambian test listings with zero Wanted images and rental-only property`() {
        val provider = DevFixturesProviderFactory.create()
        assertTrue(provider.isSupported)
        val fixtures = provider.buildFixtures()
        assertEquals(120, fixtures.size)

        val counts = fixtures.groupingBy { it.categoryId }.eachCount()
        assertEquals(35, counts["marketplace"])
        assertEquals(20, counts["property"])
        assertEquals(20, counts["services"])
        assertEquals(15, counts["jobs"])
        assertEquals(15, counts["wanted"])
        assertEquals(15, counts["swaps"])

        // Wanted fixtures must have ZERO images
        assertTrue(fixtures.filter { it.isWanted }.all { it.images.isEmpty() })
        // Property fixtures must strictly pass rental-only validation
        assertTrue(fixtures.filter { it.categoryId == "property" }.all { it.isEligiblePublic })
        // Local cache free Wanted flag stays user-scoped
        val cache = InMemoryLocalAccountCache()
        assertFalse(cache.getLocalFreeWantedPostUsed("u1"))
        cache.markLocalFreeWantedPostUsed("u1")
        assertTrue(cache.getLocalFreeWantedPostUsed("u1"))
        assertFalse(cache.getLocalFreeWantedPostUsed("u2"))
    }
}
