package com.example

import com.example.core.util.MotivationalHistoryStore
import com.example.core.util.MotivationalMessageCatalog
import com.example.core.util.MotivationalMessageSelector
import com.example.core.util.PhoneValidator
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.AppNotification
import com.example.data.model.Listing
import com.example.data.model.PaymentRecord
import com.example.data.model.PromotionPricing
import com.example.data.model.PromotionRecord
import com.example.data.model.UserProfile
import com.example.data.notifications.AppNavigationTarget
import com.example.data.notifications.NotificationFrequencyGuard
import com.example.data.notifications.NotificationKind
import com.example.data.notifications.NotificationPreferences
import com.example.data.notifications.NotificationTargetParser
import com.example.data.repository.InMemoryLocalAccountCache
import androidx.compose.ui.graphics.luminance
import com.example.ui.components.FriendlyMessageFormatter
import com.example.ui.components.TopSnackbarType
import com.example.ui.components.resolveTopSnackbarPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EnhancementsUnitTest {

    @Test
    fun browseCategories_placesMarketplaceSecond() {
        assertEquals("marketplace", MarketZedCatalogue.homeShortcutCategoryIds[1])
        assertEquals("property", MarketZedCatalogue.homeShortcutCategoryIds[0])
        assertTrue(MarketZedCatalogue.homeShortcutCategoryIds.contains("services"))
        assertTrue(MarketZedCatalogue.homeShortcutCategoryIds.contains("swaps"))
        assertTrue(MarketZedCatalogue.homeShortcutCategoryIds.contains("free"))
    }

    @Test
    fun friendlyMessageFormatter_mapsTechnicalNetworkErrorsToReadableMessages() {
        val offline = FriendlyMessageFormatter.format(
            "java.net.UnknownHostException: Unable to resolve host marketzed.netlify.app",
            TopSnackbarType.ERROR
        )
        assertTrue(offline.contains("offline", ignoreCase = true))

        val timeout = FriendlyMessageFormatter.format(
            "SocketTimeoutException: timeout",
            TopSnackbarType.WARNING
        )
        assertTrue(timeout.contains("took too long", ignoreCase = true))

        val serverError = FriendlyMessageFormatter.format(
            "HTTP 503 Service Unavailable",
            TopSnackbarType.ERROR
        )
        assertTrue(serverError.contains("temporary hiccup", ignoreCase = true))

        val regular = FriendlyMessageFormatter.format(
            "Draft saved locally on this device.",
            TopSnackbarType.SUCCESS
        )
        assertEquals("Draft saved locally on this device.", regular)
    }

    @Test
    fun motivationalSelector_avoidsImmediateAndRecentRepetitionAcrossSessions() {
        val fakeStore = object : MotivationalHistoryStore {
            private var recent: List<String> = emptyList()
            override fun getRecentIds(): List<String> = recent
            override fun saveRecentIds(ids: List<String>) {
                recent = ids
            }
        }
        val selector = MotivationalMessageSelector(
            historyStore = fakeStore,
            pool = MotivationalMessageCatalog.messages,
            random = Random(42)
        )

        val pickedIds = mutableListOf<String>()
        var lastId: String? = null
        repeat(8) {
            val next = selector.nextMessage(excludingId = lastId)
            assertNotEquals(lastId, next.id)
            pickedIds.add(next.id)
            lastId = next.id
        }
        assertEquals(8, pickedIds.toSet().size)
    }

    @Test
    fun notificationFrequencyGuard_enforcesPreferencesDedupeAndSmartEngagementCaps() {
        val prefsAllOn = NotificationPreferences(
            generalEnabled = true,
            promotionalEnabled = true,
            smartEngagementEnabled = true
        )
        val prefsNoPromo = NotificationPreferences(
            generalEnabled = true,
            promotionalEnabled = false,
            smartEngagementEnabled = true
        )

        assertTrue(NotificationFrequencyGuard.isAllowedByPreferences(NotificationKind.ANNOUNCEMENT, prefsNoPromo))
        assertFalse(NotificationFrequencyGuard.isAllowedByPreferences(NotificationKind.PROMOTION, prefsNoPromo))
        assertFalse(NotificationFrequencyGuard.isAllowedByPreferences(NotificationKind.ENGAGEMENT, prefsNoPromo))

        assertTrue(NotificationFrequencyGuard.isDuplicate("promo-dec-1", setOf("promo-dec-1", "ann-2")))
        assertFalse(NotificationFrequencyGuard.isDuplicate("promo-dec-2", setOf("promo-dec-1")))

        val now = 1_700_000_000_000L
        val inactiveLongEnough = now - (40L * 3600L * 1000L)
        val activeRecently = now - (4L * 3600L * 1000L)

        // Too recently active -> blocked
        assertFalse(
            NotificationFrequencyGuard.canSendSmartEngagement(
                nowMillis = now,
                lastAppActiveAtMillis = activeRecently,
                recentEngagementTimestamps = emptyList(),
                preferences = prefsAllOn,
                hourOfDay = 12
            )
        )

        // Inactive > 36h, no recent engagement -> allowed
        assertTrue(
            NotificationFrequencyGuard.canSendSmartEngagement(
                nowMillis = now,
                lastAppActiveAtMillis = inactiveLongEnough,
                recentEngagementTimestamps = emptyList(),
                preferences = prefsAllOn,
                hourOfDay = 12
            )
        )

        // Sent < 48h ago -> blocked
        assertFalse(
            NotificationFrequencyGuard.canSendSmartEngagement(
                nowMillis = now,
                lastAppActiveAtMillis = inactiveLongEnough,
                recentEngagementTimestamps = listOf(now - (24L * 3600L * 1000L)),
                preferences = prefsAllOn,
                hourOfDay = 12
            )
        )

        // Weekly cap reached (2 within 7 days even if older than 48h) -> blocked
        assertFalse(
            NotificationFrequencyGuard.canSendSmartEngagement(
                nowMillis = now,
                lastAppActiveAtMillis = inactiveLongEnough,
                recentEngagementTimestamps = listOf(
                    now - (72L * 3600L * 1000L),
                    now - (120L * 3600L * 1000L)
                ),
                preferences = prefsAllOn,
                hourOfDay = 12
            )
        )
    }

    @Test
    fun notificationTargetParser_resolvesPayloadsToExpectedScreens() {
        val listingTarget = NotificationTargetParser.parsePayload(
            mapOf("listingId" to "abc-123", "type" to "engagement")
        )
        assertEquals(AppNavigationTarget.ListingDetail("abc-123"), listingTarget)

        val categoryTarget = NotificationTargetParser.parsePayload(
            mapOf("categoryId" to "marketplace")
        )
        assertEquals(AppNavigationTarget.Category("marketplace"), categoryTarget)

        val announcementTarget = NotificationTargetParser.parsePayload(
            mapOf(
                "type" to "admin_broadcast",
                "title" to "System Maintenance",
                "body" to "Scheduled upgrade tonight",
                "notificationId" to "ann-9"
            )
        )
        assertTrue(announcementTarget is AppNavigationTarget.Announcement)
        assertEquals("ann-9", (announcementTarget as AppNavigationTarget.Announcement).id)

        val exploreTarget = NotificationTargetParser.parsePayload(
            mapOf("route" to "explore", "query" to "laptop")
        )
        assertEquals(AppNavigationTarget.Explore("laptop"), exploreTarget)

        val promoTarget = NotificationTargetParser.parsePayload(
            mapOf("type" to "promotion")
        )
        assertEquals(AppNavigationTarget.Promotions, promoTarget)
    }

    @Test
    fun zambianPhoneValidator_normalizesLocalNineAndTenDigitInputsTo260() {
        assertEquals("260969333123", PhoneValidator.normalizeZambianNumber("969333123"))
        assertEquals("260969331234", PhoneValidator.normalizeZambianNumber("0969331234"))
        assertEquals("260969331234", PhoneValidator.normalizeZambianNumber("260969331234"))
        assertEquals("260969331234", PhoneValidator.normalizeZambianNumber("+260969331234"))
        assertEquals("260969331234", PhoneValidator.normalizeZambianNumber("00260 969-331-234"))
        assertEquals("260969331234", PhoneValidator.normalizeZambianNumber("2600969331234"))
        assertEquals("260971234567", PhoneValidator.normalizeZambianNumber("97 123-4567"))

        assertEquals("260969333123", PhoneValidator.autoFixZambianInput("969333123"))
        assertEquals("260969331234", PhoneValidator.autoFixZambianInput("0969331234"))
        assertEquals("0969", PhoneValidator.autoFixZambianInput("0969"))

        assertTrue(PhoneValidator.isValidZambianPhone("969333123"))
        assertTrue(PhoneValidator.isValidZambianPhone("0969331234"))
        assertTrue(PhoneValidator.isValidZambianPhone("969333"))
        assertFalse(PhoneValidator.isValidZambianPhone("123"))
        assertNull(PhoneValidator.normalizeZambianNumber("+14155550199"))
    }

    @Test
    fun motivationalScheduleGuard_respectsPreferenceIdleWindowAndDailyCap() {
        val now = 1_700_000_000_000L
        val prefsOn = NotificationPreferences(motivationalEnabled = true)
        val prefsOff = NotificationPreferences(motivationalEnabled = false)

        // Disabled by user preference -> blocked
        assertFalse(
            NotificationFrequencyGuard.canSendMotivationalNotification(
                nowMillis = now,
                lastAppActiveAtMillis = now - (10L * 3600L * 1000L),
                lastMotivationalSentAtMillis = 0L,
                preferences = prefsOff,
                hourOfDay = 12
            )
        )

        // App was active 1 hour ago (< 4h idle) -> blocked
        assertFalse(
            NotificationFrequencyGuard.canSendMotivationalNotification(
                nowMillis = now,
                lastAppActiveAtMillis = now - (1L * 3600L * 1000L),
                lastMotivationalSentAtMillis = 0L,
                preferences = prefsOn,
                hourOfDay = 12
            )
        )

        // Sent 6 hours ago (< 20h interval) -> blocked
        assertFalse(
            NotificationFrequencyGuard.canSendMotivationalNotification(
                nowMillis = now,
                lastAppActiveAtMillis = now - (10L * 3600L * 1000L),
                lastMotivationalSentAtMillis = now - (6L * 3600L * 1000L),
                preferences = prefsOn,
                hourOfDay = 12
            )
        )

        // Closed >= 4h and not sent in last 20h -> allowed
        assertTrue(
            NotificationFrequencyGuard.canSendMotivationalNotification(
                nowMillis = now,
                lastAppActiveAtMillis = now - (8L * 3600L * 1000L),
                lastMotivationalSentAtMillis = now - (24L * 3600L * 1000L),
                preferences = prefsOn,
                hourOfDay = 12
            )
        )
    }

    @Test
    fun localAccountCache_storesUserScopedDataAndClearsOnSignOut() {
        val cache = InMemoryLocalAccountCache()
        val scopeA = "user_a"
        val scopeB = "user_b"

        val profileA = UserProfile(
            firstName = "Chanda",
            lastName = "Mwamba",
            verificationPaymentStatus = "paid",
            postingEnabled = true
        )
        val listingA = Listing(
            id = "lst-1",
            title = "iPhone 14",
            categoryId = "marketplace"
        )
        val notifA = AppNotification(
            id = "n-1",
            type = "listing_published_pending_review",
            title = "Listing Published",
            message = "Your listing is live.",
            read = false
        )
        val paymentA = PaymentRecord(
            id = "pay-1",
            purpose = "account_verification",
            amount = 30.0,
            status = "successful"
        )
        val promoA = PromotionRecord(
            id = "pr-1",
            listingId = "lst-1",
            durationDays = 7,
            status = "active"
        )
        val pricing = PromotionPricing(
            prices = mapOf("7" to 25.0, "14" to 45.0, "30" to 80.0)
        )

        cache.saveProfile(scopeA, profileA)
        cache.saveMyListings(scopeA, listOf(listingA))
        cache.saveNotifications(scopeA, listOf(notifA))
        cache.savePayments(scopeA, listOf(paymentA))
        cache.savePromotions(scopeA, listOf(promoA))
        cache.savePromotionPricing(pricing)

        assertEquals("Chanda", cache.getProfile(scopeA)?.firstName)
        assertTrue(cache.getProfile(scopeA)?.isPostingActivated == true)
        assertEquals(1, cache.getMyListings(scopeA).size)
        assertEquals(1, cache.getNotifications(scopeA).size)
        assertEquals(1, cache.getPayments(scopeA).size)
        assertEquals(1, cache.getPromotions(scopeA).size)
        assertEquals(25.0, cache.getPromotionPricing()?.prices?.get("7"))

        // Other user scope stays isolated
        assertNull(cache.getProfile(scopeB))
        assertTrue(cache.getMyListings(scopeB).isEmpty())

        // Clearing scopeA removes user data but keeps global promotion pricing
        cache.clearUserScope(scopeA)
        assertNull(cache.getProfile(scopeA))
        assertTrue(cache.getMyListings(scopeA).isEmpty())
        assertTrue(cache.getNotifications(scopeA).isEmpty())
        assertTrue(cache.getPayments(scopeA).isEmpty())
        assertTrue(cache.getPromotions(scopeA).isEmpty())
        assertEquals(45.0, cache.getPromotionPricing()?.prices?.get("14"))
    }

    @Test
    fun topSnackbarPalette_providesHighContrastForLightAndDarkModes() {
        TopSnackbarType.entries.forEach { type ->
            val light = resolveTopSnackbarPalette(type = type, isDark = false)
            val dark = resolveTopSnackbarPalette(type = type, isDark = true)

            assertNotEquals(light.containerColor, dark.containerColor)
            assertNotEquals(light.contentColor, dark.contentColor)
            assertTrue(light.containerColor.luminance() > 0.6f)
            assertTrue(light.contentColor.luminance() < 0.25f)
            assertTrue(dark.containerColor.luminance() < 0.1f)
            assertTrue(dark.contentColor.luminance() > 0.7f)
            assertTrue(dark.accentColor.luminance() > light.accentColor.luminance())
        }
    }
}
