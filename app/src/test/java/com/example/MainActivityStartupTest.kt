package com.example

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityStartupTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity launches and navigates across all 5 tabs without crashing`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("MarketZed").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_explore").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_post").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_wanted").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_account").assertIsDisplayed()

        // Navigate through all tabs to verify every top-level Compose tree renders cleanly
        composeRule.onNodeWithTag("nav_explore").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("explore_search_input").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_post").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign in to post on MarketZed").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_wanted").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Wanted Requests").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign in to MarketZed").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("MarketZed").assertIsDisplayed()
    }

    @Test
    fun `status and navigation bars are transparent with dark icons and text`() {
        composeRule.waitForIdle()
        val window = composeRule.activity.window
        @Suppress("DEPRECATION")
        run {
            assertEquals(AndroidColor.TRANSPARENT, window.statusBarColor)
            assertEquals(AndroidColor.TRANSPARENT, window.navigationBarColor)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertFalse(window.isStatusBarContrastEnforced)
            assertFalse(window.isNavigationBarContrastEnforced)
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        assertTrue(controller.isAppearanceLightStatusBars)
        assertTrue(controller.isAppearanceLightNavigationBars)
    }

    @Test
    fun `app resources including font and google services web client id load cleanly`() {
        val context = ApplicationProvider.getApplicationContext<MarketZedApplication>()
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        assert(resId != 0)
        assertNotNull(ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, null))
        assertNotNull(ResourcesCompat.getFont(context, R.font.poppins))
    }
}
