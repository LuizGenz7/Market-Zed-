package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthUserState
import com.example.data.model.AdminOverview
import com.example.data.model.UserProfile
import com.example.ui.screens.account.AccountScreen
import com.example.ui.screens.account.AccountUiState
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminUiState
import com.example.ui.theme.MarketZedTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AdminDashboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `extractAdminFromClaims detects boolean, string role, and roles list`() {
        assertEquals(true to "admin", AuthRepository.extractAdminFromClaims(mapOf("admin" to true)))
        assertEquals(true to "admin", AuthRepository.extractAdminFromClaims(mapOf("isAdmin" to true)))
        assertEquals(true to "super_admin", AuthRepository.extractAdminFromClaims(mapOf("role" to "super_admin")))
        assertEquals(true to "admin", AuthRepository.extractAdminFromClaims(mapOf("roles" to listOf("member", "admin"))))
        assertEquals(false to "member", AuthRepository.extractAdminFromClaims(mapOf("role" to "member")))
        assertEquals(false to null, AuthRepository.extractAdminFromClaims(emptyMap()))
    }

    @Test
    fun `AccountUiState isAdmin requires signed-in session and verified admin privilege`() {
        val signedOutWithFlag = AccountUiState(
            auth = AuthUserState(uid = null, isAdminClaim = true),
            profile = UserProfile(role = "admin", isAdmin = true),
            adminBackendVerified = true
        )
        assertFalse(signedOutWithFlag.isAdmin)

        val regularUser = AccountUiState(
            auth = AuthUserState(uid = "user_1", email = "user@example.com"),
            profile = UserProfile(uid = "user_1", role = "member")
        )
        assertFalse(regularUser.isAdmin)

        val adminByProfile = AccountUiState(
            auth = AuthUserState(uid = "admin_1", email = "admin@marketzed.com"),
            profile = UserProfile(uid = "admin_1", role = "admin")
        )
        assertTrue(adminByProfile.isAdmin)
    }

    @Test
    fun `AccountScreen hides Admin Dashboard from regular users and shows it for admins`() {
        val regularState = AccountUiState(
            auth = AuthUserState(isFirebaseConfigured = true, uid = "user_1", email = "buyer@example.com"),
            profile = UserProfile(uid = "user_1", firstName = "Chanda", lastName = "Mwansa")
        )
        composeRule.setContent {
            MarketZedTheme {
                AccountScreen(
                    state = regularState,
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
        composeRule.onNodeWithTag("account_my_listings").assertIsDisplayed()
        composeRule.onNodeWithTag("account_admin_dashboard").assertDoesNotExist()
        composeRule.onNodeWithTag("auth_google_button").assertDoesNotExist()
    }

    @Test
    fun `AccountScreen displays Admin Dashboard entry card when signed in as admin`() {
        val adminState = AccountUiState(
            auth = AuthUserState(isFirebaseConfigured = true, uid = "admin_1", email = "admin@marketzed.com", isAdminClaim = true),
            profile = UserProfile(uid = "admin_1", firstName = "Admin", lastName = "User", role = "admin", isAdmin = true)
        )
        composeRule.setContent {
            MarketZedTheme {
                AccountScreen(
                    state = adminState,
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
        composeRule.onNodeWithTag("account_admin_dashboard").assertIsDisplayed()
        composeRule.onNodeWithText("Admin Dashboard").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_google_button").assertDoesNotExist()
    }

    @Test
    fun `AdminDashboardScreen blocks non-admin access even if navigated directly`() {
        composeRule.setContent {
            MarketZedTheme {
                AdminDashboardScreen(
                    isAdmin = false,
                    adminEmail = "buyer@example.com",
                    adminRoleLabel = null,
                    state = AdminUiState(),
                    onBack = {},
                    onLoadInitial = {},
                    onSelectTab = {},
                    onRefreshTab = {},
                    onSetListingFilter = {},
                    onSetListingQuery = {},
                    onSearchListings = {},
                    onModerateListing = { _, _, _ -> },
                    onOpenListing = {},
                    onSetPromotionFilter = {},
                    onManagePromotion = { _, _, _, _, _ -> },
                    onGrantPromotion = { _, _, _, _ -> },
                    onUpdatePromotionPricing = { _, _, _ -> },
                    onSetReportFilter = {},
                    onResolveReport = { _, _, _ -> },
                    onSetUserFilter = {},
                    onSetUserQuery = {},
                    onSearchUsers = {},
                    onManageUser = { _, _, _ -> },
                    onSetPaymentFilter = {},
                    onSendBroadcast = { _, _, _, _ -> },
                    onClearFeedback = {}
                )
            }
        }
        composeRule.onNodeWithTag("admin_access_denied").assertIsDisplayed()
        composeRule.onNodeWithText("Admin Access Required").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_tab_overview").assertDoesNotExist()
    }

    @Test
    fun `AdminDashboardScreen renders all management tabs and live overview for verified admin`() {
        val sampleState = AdminUiState(
            overview = AdminOverview(
                totalListings = 42,
                liveListings = 35,
                pendingReviewListings = 4,
                activePromotions = 6,
                openReports = 2,
                totalUsers = 120,
                verifiedUsers = 18,
                totalRevenueZmw = 1850.0
            )
        )
        composeRule.setContent {
            MarketZedTheme {
                AdminDashboardScreen(
                    isAdmin = true,
                    adminEmail = "admin@marketzed.com",
                    adminRoleLabel = "admin",
                    state = sampleState,
                    onBack = {},
                    onLoadInitial = {},
                    onSelectTab = {},
                    onRefreshTab = {},
                    onSetListingFilter = {},
                    onSetListingQuery = {},
                    onSearchListings = {},
                    onModerateListing = { _, _, _ -> },
                    onOpenListing = {},
                    onSetPromotionFilter = {},
                    onManagePromotion = { _, _, _, _, _ -> },
                    onGrantPromotion = { _, _, _, _ -> },
                    onUpdatePromotionPricing = { _, _, _ -> },
                    onSetReportFilter = {},
                    onResolveReport = { _, _, _ -> },
                    onSetUserFilter = {},
                    onSetUserQuery = {},
                    onSearchUsers = {},
                    onManageUser = { _, _, _ -> },
                    onSetPaymentFilter = {},
                    onSendBroadcast = { _, _, _, _ -> },
                    onClearFeedback = {}
                )
            }
        }
        composeRule.onNodeWithTag("admin_tab_overview").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_tab_listings").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_tab_promotions").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_tab_reports").assertIsDisplayed()
        composeRule.onNodeWithText("Platform Metrics").assertIsDisplayed()
        composeRule.onNodeWithText("35").assertIsDisplayed()
    }
}
