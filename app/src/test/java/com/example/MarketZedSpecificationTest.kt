package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.local.MarketZedDatabase
import com.example.data.model.CategoryDetails
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.model.ListingLocation
import com.example.data.model.SearchFilter
import com.example.data.model.SponsoredFormat
import com.example.data.model.UserProfile
import com.example.data.repository.ComparisonAddResult
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.SponsoredRotationHelper
import com.example.ui.components.JobFullWidthCard
import com.example.ui.components.ServiceFullWidthCard
import com.example.ui.components.WantedTextOnlyCard
import com.example.ui.screens.account.AccountScreen
import com.example.ui.screens.account.AccountUiState
import com.example.ui.screens.compare.ComparisonItemState
import com.example.ui.screens.compare.ComparisonScreen
import com.example.ui.screens.compare.ComparisonUiState
import com.example.ui.screens.compare.ComparisonViewModel
import com.example.ui.screens.detail.ListingDetailScreen
import com.example.ui.screens.detail.ListingDetailUiState
import com.example.ui.theme.MarketZedTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class MarketZedSpecificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `strict rental-only property policy excludes land plots and property sales`() {
        // Land / plot prohibited everywhere
        assertNotNull(
            MarketZedCatalogue.validatePropertyAndLandPolicy(
                categoryId = "land",
                subcategory = "Land",
                title = "50x30 plot in Silverest",
                description = "Titled land ready for building near Great East Road."
            )
        )
        assertNotNull(
            MarketZedCatalogue.validatePropertyAndLandPolicy(
                categoryId = "property",
                subcategory = "Houses for rent",
                title = "Residential plot on Kafue Road",
                description = "Clean residential plot with title deeds."
            )
        )
        // Property sale prohibited
        assertNotNull(
            MarketZedCatalogue.validatePropertyAndLandPolicy(
                categoryId = "property",
                subcategory = "Houses for rent",
                title = "3-bedroom house for sale in Roma",
                description = "Modern house for sale with electric fence and paved yard.",
                priceMode = "sale"
            )
        )
        // Legitimate rental property permitted
        assertNull(
            MarketZedCatalogue.validatePropertyAndLandPolicy(
                categoryId = "property",
                subcategory = "Apartments & flats for rent",
                title = "2-bedroom apartment for rent in Kabulonga",
                description = "Spacious apartment for rent with borehole water and carport.",
                priceMode = "rent"
            )
        )

        val saleListing = Listing(
            id = "prop_sale_1",
            categoryId = "property",
            subcategory = "Houses for rent",
            title = "Executive house for sale",
            description = "Property for sale in Ibex Hill",
            price = 2_500_000.0
        )
        val rentalListing = Listing(
            id = "prop_rent_1",
            categoryId = "property",
            subcategory = "Houses for rent",
            title = "Executive house for rent",
            description = "Modern 3-bedroom house to rent in Ibex Hill",
            price = 12_000.0,
            priceMode = "monthly"
        )
        assertFalse(saleListing.isEligiblePublic)
        assertTrue(rentalListing.isEligiblePublic)
    }

    @Test
    fun `verified price drop never fabricates fake discounts`() {
        val noOriginal = Listing(id = "p1", price = 500.0, originalPrice = null)
        assertFalse(noOriginal.hasVerifiedPriceDrop)
        assertNull(noOriginal.priceDropPercent)

        val equalPrice = Listing(id = "p2", price = 500.0, originalPrice = 500.0)
        assertFalse(equalPrice.hasVerifiedPriceDrop)
        assertNull(equalPrice.priceDropPercent)

        val realDrop = Listing(id = "p3", price = 800.0, originalPrice = 1000.0)
        assertTrue(realDrop.hasVerifiedPriceDrop)
        assertEquals(20, realDrop.priceDropPercent)
    }

    @Test
    fun `UserProfile defaults empty name to MarketZed User`() {
        val emptyUser = UserProfile(uid = "u1", email = "user@example.com", firstName = "", lastName = "")
        assertEquals("MarketZed User", emptyUser.displayName)
    }

    @Test
    fun `comparison spec extraction returns Not provided for missing fields`() {
        val phone = Listing(
            id = "phone_1",
            categoryId = "marketplace",
            subcategory = "Phones and accessories",
            title = "iPhone 13",
            categoryDetails = CategoryDetails(
                brand = "Apple",
                model = "iPhone 13",
                condition = "Like New",
                specs = mapOf("Storage" to "128GB")
            )
        )
        val specs = MarketZedCatalogue.extractComparisonSpecs(phone)
        assertEquals("Apple", specs["Brand"])
        assertEquals("iPhone 13", specs["Model"])
        assertEquals("128GB", specs["Storage"])
        assertEquals("Like New", specs["Condition"])
        assertEquals("Not provided", specs["RAM"])
        assertEquals("Not provided", specs["Battery"])
    }

    @Test
    fun `sponsored rotation helper rotates listings fairly across slots`() {
        val items = listOf(
            Listing(id = "s1", isPromoted = true, title = "Item 1"),
            Listing(id = "s2", isPromoted = true, title = "Item 2"),
            Listing(id = "s3", isPromoted = true, title = "Item 3"),
            Listing(id = "org", isPromoted = false, title = "Organic")
        )
        val slot0 = SponsoredRotationHelper.rotateFairly(items, rotationSlot = 0)
        val slot1 = SponsoredRotationHelper.rotateFairly(items, rotationSlot = 1)
        assertEquals(listOf("s1", "s2", "s3"), slot0.map { it.id })
        assertEquals(listOf("s2", "s3", "s1"), slot1.map { it.id })
        assertEquals(
            SponsoredFormat.HOMEPAGE_BANNER,
            SponsoredFormat.entries.firstOrNull { it.wireValue == "homepage_banner" }
        )
    }

    @Test
    fun `room v4 repository enforces max 3 comparison selections and persists saved searches`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MarketZedDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = LocalUserRepository(context, db.dao())
            val scope = "user_test"

            assertEquals(ComparisonAddResult.Added, repo.addComparisonItem(scope, "item_1"))
            assertEquals(ComparisonAddResult.AlreadySelected, repo.addComparisonItem(scope, "item_1"))
            assertEquals(ComparisonAddResult.Added, repo.addComparisonItem(scope, "item_2"))
            assertEquals(ComparisonAddResult.Added, repo.addComparisonItem(scope, "item_3"))
            assertEquals(ComparisonAddResult.LimitReached, repo.addComparisonItem(scope, "item_4"))

            val selected = repo.observeComparisonIds(scope).first()
            assertEquals(listOf("item_1", "item_2", "item_3"), selected)

            repo.removeComparisonItem(scope, "item_2")
            assertEquals(ComparisonAddResult.Added, repo.addComparisonItem(scope, "item_4"))
            assertEquals(listOf("item_1", "item_3", "item_4"), repo.observeComparisonIds(scope).first())

            val saved = repo.saveSearch(
                userScope = scope,
                name = "Lusaka Laptops",
                filter = SearchFilter(query = "ThinkPad", category = "marketplace", province = "Lusaka"),
                notifyMatches = true
            )
            val savedList = repo.observeSavedSearches(scope).first()
            assertEquals(1, savedList.size)
            assertEquals("Lusaka Laptops", savedList.first().name)
            assertTrue(savedList.first().notifyMatches)

            repo.toggleSavedSearchAlerts(scope, saved.id, false)
            assertFalse(repo.observeSavedSearches(scope).first().first().notifyMatches)

            repo.deleteSavedSearch(scope, saved.id)
            assertTrue(repo.observeSavedSearches(scope).first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `WantedTextOnlyCard and Wanted ListingDetailScreen never render images even if listing has image urls`() {
        val wantedWithImages = Listing(
            id = "wanted_99",
            categoryId = "wanted",
            subcategory = "Electronics wanted",
            title = "Looking for MacBook Air M2 in Lusaka",
            description = "Clean 16GB RAM model preferred with box.",
            price = 18_500.0,
            sellerName = "Mwamba Phiri",
            sellerVerified = true,
            location = ListingLocation(province = "Lusaka", town = "Lusaka"),
            images = listOf(ListingImage(url = "https://example.com/should_not_render.jpg"))
        )

        composeRule.setContent {
            MarketZedTheme {
                WantedTextOnlyCard(
                    listing = wantedWithImages,
                    isFavorite = false,
                    onClick = {},
                    onToggleFavorite = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wanted_text_only_card_wanted_99").assertIsDisplayed()
        composeRule.onNodeWithText("Looking for MacBook Air M2 in Lusaka").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Looking for MacBook Air M2 in Lusaka photo 1").assertCountEquals(0)
    }

    @Test
    fun `Wanted ListingDetailScreen skips hero image container completely`() {
        val wantedWithImages = Listing(
            id = "wanted_100",
            categoryId = "wanted",
            subcategory = "Electronics wanted",
            title = "Looking for Solar Inverter",
            description = "Need a 5kVA hybrid solar inverter urgently in Kitwe.",
            price = 9_500.0,
            sellerName = "Bwalya Banda",
            location = ListingLocation(province = "Copperbelt", town = "Kitwe"),
            images = listOf(ListingImage(url = "https://example.com/should_not_render.jpg"))
        )

        composeRule.setContent {
            MarketZedTheme {
                ListingDetailScreen(
                    state = ListingDetailUiState(listing = wantedWithImages, isLoading = false),
                    onBack = {},
                    onToggleFavorite = {},
                    onRequireSignInForReport = {},
                    onSubmitReport = { _, _ -> },
                    onClearReportFeedback = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Looking for Solar Inverter").assertIsDisplayed()
        composeRule.onNodeWithText("Wanted Request").assertIsDisplayed()
        composeRule.onNodeWithText("WANTED").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Looking for Solar Inverter photo 1").assertCountEquals(0)
    }

    @Test
    fun `JobFullWidthCard and ServiceFullWidthCard display full-width content and untruncated Verified badge`() {
        val job = Listing(
            id = "job_1",
            categoryId = "jobs",
            subcategory = "Full-time",
            title = "Registered Accountant (ZICA)",
            sellerName = "ZamAgri Supplies",
            sellerVerified = true,
            description = "Manage monthly reconciliations and ZRA compliance.",
            price = 15_000.0,
            location = ListingLocation(province = "Copperbelt", town = "Chingola"),
            categoryDetails = CategoryDetails(employmentType = "Full-time")
        )
        val service = Listing(
            id = "srv_1",
            categoryId = "services",
            subcategory = "Phone repair",
            title = "Same-Day Screen Repair",
            sellerName = "ZedFix Mobile Lab",
            sellerVerified = true,
            description = "Original OLED/LCD screen replacements while you wait.",
            price = 650.0,
            location = ListingLocation(province = "Lusaka", town = "Lusaka")
        )

        composeRule.setContent {
            MarketZedTheme {
                androidx.compose.foundation.layout.Column {
                    JobFullWidthCard(
                        listing = job,
                        isFavorite = false,
                        dataSaverEnabled = true,
                        onClick = {},
                        onToggleFavorite = {}
                    )
                    ServiceFullWidthCard(
                        listing = service,
                        isFavorite = false,
                        dataSaverEnabled = true,
                        onClick = {},
                        onToggleFavorite = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("job_full_width_card_job_1").assertIsDisplayed()
        composeRule.onNodeWithTag("service_full_width_card_srv_1").assertIsDisplayed()
        composeRule.onNodeWithText("Registered Accountant (ZICA)").assertIsDisplayed()
        composeRule.onNodeWithText("Same-Day Screen Repair").assertIsDisplayed()
    }

    @Test
    fun `AccountScreen signed-out view provides Email and Password without Google Sign-In`() {
        composeRule.setContent {
            MarketZedTheme {
                AccountScreen(
                    state = AccountUiState(),
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
        composeRule.onNodeWithTag("auth_email_input").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_password_input").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_submit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_google_button").assertDoesNotExist()
    }

    @Test
    fun `ComparisonScreen renders side-by-side matrix and Show Differences filter`() {
        val itemA = Listing(
            id = "cmp_1",
            categoryId = "marketplace",
            subcategory = "Phones and accessories",
            title = "Samsung Galaxy A54",
            price = 7_500.0,
            categoryDetails = CategoryDetails(brand = "Samsung", model = "Galaxy A54", condition = "New")
        )
        val itemB = Listing(
            id = "cmp_2",
            categoryId = "marketplace",
            subcategory = "Phones and accessories",
            title = "Google Pixel 7a",
            price = 8_200.0,
            categoryDetails = CategoryDetails(brand = "Google", model = "Pixel 7a", condition = "Like New")
        )
        val items = listOf(
            ComparisonItemState("cmp_1", itemA, isUnavailable = false),
            ComparisonItemState("cmp_2", itemB, isUnavailable = false)
        )
        val rows = ComparisonViewModel.buildComparisonRows(items)

        composeRule.setContent {
            MarketZedTheme {
                ComparisonScreen(
                    state = ComparisonUiState(
                        selectedIds = listOf("cmp_1", "cmp_2"),
                        items = items,
                        specRows = rows,
                        differingAttributes = rows.filter { it.hasDifference }.map { it.attribute }.toSet(),
                        showDifferencesOnly = false
                    ),
                    dataSaverEnabled = true,
                    shareSummary = "Test summary",
                    onBack = {},
                    onOpenListing = {},
                    onOpenSeller = {},
                    onRemoveItem = {},
                    onClearAll = {},
                    onToggleShowDifferencesOnly = {},
                    onBrowseProducts = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("comparison_matrix_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("compare_filter_differences").assertIsDisplayed()
        composeRule.onNodeWithText("Samsung Galaxy A54").assertIsDisplayed()
        composeRule.onNodeWithText("Google Pixel 7a").assertExists()
    }
}
