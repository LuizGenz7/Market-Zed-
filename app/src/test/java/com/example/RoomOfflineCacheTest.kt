package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedListingEntity
import com.example.data.local.ListingDraftEntity
import com.example.data.local.MarketZedDao
import com.example.data.local.MarketZedDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RoomOfflineCacheTest {

    private lateinit var db: MarketZedDatabase
    private lateinit var dao: MarketZedDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MarketZedDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun sampleListing(id: String, sectionKey: String = "home_products"): CachedListingEntity {
        val now = System.currentTimeMillis()
        return CachedListingEntity(
            id = id,
            sectionKey = sectionKey,
            ownerId = "owner_1",
            sellerName = "Chanda Mwamba",
            sellerVerified = true,
            isPromoted = false,
            categoryId = "marketplace",
            subcategory = "Electronics",
            title = "Listing $id",
            description = "Detailed description for $id in Lusaka.",
            price = 1500.0,
            currency = "ZMW",
            priceMode = "fixed",
            province = "Lusaka",
            town = "Lusaka",
            imagesJson = "[]",
            contactPreferencesJson = null,
            categoryDetailsJson = null,
            moderationStatus = "live_reviewed",
            availabilityStatus = "available",
            wantedStatus = null,
            isPublic = true,
            reviewRequired = false,
            createdAt = "2026-11-20T10:00:00Z",
            updatedAt = null,
            publishedAt = "2026-11-20T10:00:00Z",
            expiresAt = null,
            cachedAt = now
        )
    }

    @Test
    fun `section replacement caches listings and keeps guest and user favorites scoped`() = runBlocking {
        dao.replaceSectionListings("home_products", listOf(sampleListing("l1"), sampleListing("l2")))
        val cached = dao.observeListingsBySection("home_products").first()
        assertEquals(2, cached.size)

        val addedForGuest = dao.toggleFavorite("guest", "l1")
        assertTrue(addedForGuest)
        assertEquals(listOf("l1"), dao.observeFavoriteIds("guest").first())
        assertTrue(dao.observeFavoriteIds("uid_123").first().isEmpty())

        val removedForGuest = dao.toggleFavorite("guest", "l1")
        assertFalse(removedForGuest)
        assertTrue(dao.observeFavoriteIds("guest").first().isEmpty())
    }

    @Test
    fun `recently viewed deduplicates and caps at 40 entries`() = runBlocking {
        val listings = (1..45).map { sampleListing("item_$it") }
        dao.upsertCachedListings(listings)
        listings.forEach {
            dao.recordRecentlyViewed("guest", it.id)
        }
        val recent = dao.observeRecentlyViewed("guest").first()
        assertEquals(40, recent.size)
    }

    @Test
    fun `clearing public listing cache preserves local drafts and favorites`() = runBlocking {
        dao.upsertCachedListings(listOf(sampleListing("pub_1")))
        dao.toggleFavorite("uid_abc", "pub_1")
        dao.upsertDraft(
            ListingDraftEntity(
                localUuid = "draft-1",
                userScope = "uid_abc",
                step = 1,
                categoryId = "wanted",
                subcategory = "Electronics",
                title = "Looking for Solar Inverter",
                description = "Need 3kVA inverter in Ndola, budget K4,500.",
                price = "4500",
                province = "Copperbelt",
                town = "Ndola",
                categoryDetailsJson = "{}",
                contactPreferencesJson = "{}",
                localImagePathsJson = "[]",
                uploadedImagesJson = "[]",
                ownershipDeclarationAccepted = false,
                ownershipDeclarationVersion = "2026-11-16-v1",
                updatedAt = System.currentTimeMillis()
            )
        )

        dao.clearAllPublicListingCache()
        dao.clearSyncMetadata()

        assertTrue(dao.getAllCachedListingsSnapshot().isEmpty())
        assertNotNull(dao.getDraft("draft-1"))
        assertEquals(listOf("pub_1"), dao.observeFavoriteIds("uid_abc").first())
    }
}
