package com.example

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CachedListingEntity
import com.example.data.local.ListingDraftEntity
import com.example.data.local.MarketZedDao
import com.example.data.local.MarketZedDatabase
import com.example.data.model.AdminOverview
import com.example.data.model.AdminPaymentRecord
import com.example.data.model.AdminPromotionRecord
import com.example.data.model.AdminReportRecord
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppNotification
import com.example.data.model.Listing
import com.example.data.model.ListingLocation
import com.example.data.model.PaymentRecord
import com.example.data.model.PromotionPricing
import com.example.data.model.PromotionRecord
import com.example.data.model.UserProfile
import com.example.data.remote.AdminBroadcastRequestDto
import com.example.data.remote.AdminGrantPromotionRequestDto
import com.example.data.remote.AdminListingModerationRequestDto
import com.example.data.remote.AdminPromotionManageRequestDto
import com.example.data.remote.AdminReportResolveRequestDto
import com.example.data.remote.AdminUpdatePricingRequestDto
import com.example.data.remote.AdminUserManageRequestDto
import com.example.data.remote.CloudinaryUploadResponseDto
import com.example.data.remote.ConnectivityObserver
import com.example.data.remote.CreatePaymentRequestDto
import com.example.data.remote.ItemsWrapperDto
import com.example.data.remote.ManageListingRequestDto
import com.example.data.remote.MarkNotificationReadRequestDto
import com.example.data.remote.MarketZedApiService
import com.example.data.remote.PaymentResponseDto
import com.example.data.remote.PaymentStatusRequestDto
import com.example.data.remote.ProfileUpdateRequestDto
import com.example.data.remote.ReportListingRequestDto
import com.example.data.remote.ReviseListingRequestDto
import com.example.data.remote.SearchResponseDto
import com.example.data.remote.SubmitListingRequestDto
import com.example.data.remote.SubmitListingResponseDto
import com.example.data.remote.TestimonialRequestDto
import com.example.data.repository.MarketplaceRepository
import com.example.data.sync.MarketZedWorkerFactory
import com.example.data.sync.MarketplaceSyncScheduler
import com.example.data.sync.MarketplaceSyncWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MarketplaceSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var db: MarketZedDatabase
    private lateinit var dao: MarketZedDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MarketZedDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun sampleApiListing(id: String, categoryId: String, title: String): Listing {
        return Listing(
            id = id,
            sellerName = "Bwalya Phiri",
            sellerVerified = true,
            isPromoted = false,
            categoryId = categoryId,
            subcategory = "Electronics",
            title = title,
            description = "Clean listing in Lusaka ready for pickup.",
            price = 2500.0,
            currency = "ZMW",
            priceMode = "fixed",
            location = ListingLocation(province = "Lusaka", town = "Lusaka"),
            moderationStatus = "live_reviewed",
            availabilityStatus = "available",
            isPublic = true,
            createdAt = "2026-11-20T12:00:00.000Z",
            publishedAt = "2026-11-20T12:00:00.000Z"
        )
    }

    private fun staleCachedSearchRow(
        id: String,
        cachedAt: Long
    ): CachedListingEntity {
        return CachedListingEntity(
            id = id,
            sectionKey = "search:solar||Lusaka|newest||",
            ownerId = "owner_1",
            sellerName = "Seller",
            sellerVerified = false,
            isPromoted = false,
            categoryId = "marketplace",
            subcategory = "Electronics",
            title = "Search Result $id",
            description = "Cached search entry",
            price = 1200.0,
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
            createdAt = "2026-11-20T12:00:00.000Z",
            updatedAt = null,
            publishedAt = "2026-11-20T12:00:00.000Z",
            expiresAt = null,
            cachedAt = cachedAt
        )
    }

    private fun buildWorker(
        repo: MarketplaceRepository,
        forceRefresh: Boolean = true,
        respectWifiPref: Boolean = true,
        unmeteredNetwork: Boolean = true,
        runAttemptCount: Int = 0
    ): MarketplaceSyncWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return MarketplaceSyncWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    marketplaceRepository = repo,
                    dao = dao,
                    unmeteredNetworkChecker = { unmeteredNetwork }
                )
            }
        }
        return TestListenableWorkerBuilder<MarketplaceSyncWorker>(context)
            .setWorkerFactory(factory)
            .setInputData(
                workDataOf(
                    MarketplaceSyncWorker.KEY_FORCE_REFRESH to forceRefresh,
                    MarketplaceSyncWorker.KEY_RESPECT_WIFI_PREF to respectWifiPref,
                    MarketplaceSyncWorker.KEY_SYNC_SOURCE to "test"
                )
            )
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    @Test
    fun `worker syncs public search sections and pricing into Room while pruning only stale unreferenced search rows`() = runBlocking {
        val staleTime = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
        dao.upsertCachedListings(
            listOf(
                staleCachedSearchRow("stale_unpinned", staleTime),
                staleCachedSearchRow("stale_favorited", staleTime),
                staleCachedSearchRow("stale_recent", staleTime)
            )
        )
        dao.toggleFavorite("guest", "stale_favorited")
        dao.recordRecentlyViewed("guest", "stale_recent")
        dao.upsertDraft(
            ListingDraftEntity(
                localUuid = "draft_keep",
                userScope = "guest",
                step = 1,
                categoryId = "marketplace",
                subcategory = "Electronics",
                title = "Draft Phone",
                description = "Saved offline draft",
                price = "3000",
                province = "Lusaka",
                town = "Lusaka",
                categoryDetailsJson = "{}",
                contactPreferencesJson = "{}",
                localImagePathsJson = "[]",
                uploadedImagesJson = "[]",
                ownershipDeclarationAccepted = false,
                ownershipDeclarationVersion = "2026-11-16-v1",
                updatedAt = System.currentTimeMillis()
            )
        )

        val fakeApi = FakeMarketZedApiService(
            onSearch = { category ->
                val items = when (category) {
                    "property" -> listOf(sampleApiListing("r1", "property", "3 Bedroom House to Rent"))
                    "services" -> listOf(sampleApiListing("s1", "services", "Solar Inverter Installation"))
                    "swaps" -> listOf(sampleApiListing("sw1", "swaps", "Swap iPhone 13 for Laptop"))
                    "wanted" -> listOf(sampleApiListing("w1", "wanted", "Looking for 5kVA Inverter"))
                    "jobs" -> listOf(sampleApiListing("j1", "jobs", "Solar Technician Wanted"))
                    else -> listOf(
                        sampleApiListing("p1", "marketplace", "Samsung Galaxy A55"),
                        sampleApiListing("p2", "vehicles", "Toyota Hilux 2019")
                    )
                }
                Response.success(SearchResponseDto(items = items, nextCursor = null))
            },
            onPricing = {
                Response.success(PromotionPricing(prices = mapOf("7" to 50.0, "14" to 90.0)))
            }
        )

        val repo = MarketplaceRepository(dao, fakeApi, ConnectivityObserver(context))
        val result = buildWorker(repo).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val output = (result as ListenableWorker.Result.Success).outputData
        assertEquals(7, output.getInt(MarketplaceSyncWorker.KEY_SYNCED_LISTINGS_COUNT, -1))
        assertEquals(true, output.getBoolean(MarketplaceSyncWorker.KEY_PRICING_SYNCED, false))
        assertEquals(1, output.getInt(MarketplaceSyncWorker.KEY_PRUNED_COUNT, -1))

        assertEquals(2, dao.observeListingsBySection(MarketplaceRepository.SECTION_HOME_PRODUCTS).first().size)
        assertEquals(1, dao.observeListingsBySection(MarketplaceRepository.SECTION_HOME_PROPERTY).first().size)
        assertEquals(1, dao.observeListingsBySection(MarketplaceRepository.SECTION_HOME_SERVICES).first().size)
        assertEquals(1, dao.observeListingsBySection(MarketplaceRepository.SECTION_HOME_SWAPS).first().size)
        assertEquals(1, dao.observeListingsBySection(MarketplaceRepository.SECTION_HOME_WANTED).first().size)
        assertEquals(1, dao.observeListingsBySection(MarketplaceRepository.SECTION_HOME_JOBS).first().size)

        // Unpinned stale search row removed; favorites, recently viewed, and drafts remain safe
        assertNull(dao.getListingById("stale_unpinned"))
        assertNotNull(dao.getListingById("stale_favorited"))
        assertNotNull(dao.getListingById("stale_recent"))
        assertNotNull(dao.getDraft("draft_keep"))

        val cachedPricing = repo.getCachedPromotionPricing()
        assertEquals(50.0, cachedPricing?.prices?.get("7") ?: 0.0, 0.01)

        val workMeta = dao.getSyncMetadata(MarketplaceRepository.META_WORK_SYNC)
        assertNotNull(workMeta)
        assertNull(workMeta?.lastError)
    }

    @Test
    fun `worker skips gracefully when wifiOnlyPrefetch is enabled and active network is metered`() = runBlocking {
        dao.upsertSettings(
            AppSettingsEntity(
                themeModeName = "LIGHT",
                dataSaverEnabled = true,
                wifiOnlyPrefetch = true
            )
        )
        var apiCalled = false
        val fakeApi = FakeMarketZedApiService(
            onSearch = {
                apiCalled = true
                Response.success(SearchResponseDto())
            },
            onPricing = {
                apiCalled = true
                Response.success(PromotionPricing())
            }
        )
        val repo = MarketplaceRepository(dao, fakeApi, ConnectivityObserver(context))

        val result = buildWorker(
            repo = repo,
            respectWifiPref = true,
            unmeteredNetwork = false
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val reason = (result as ListenableWorker.Result.Success).outputData
            .getString(MarketplaceSyncWorker.KEY_SKIPPED_REASON)
        assertEquals(MarketplaceSyncWorker.SKIPPED_WIFI_ONLY, reason)
        assertEquals(false, apiCalled)
    }

    @Test
    fun `worker retries on transient network failure then fails after max attempts`() = runBlocking {
        val failingApi = FakeMarketZedApiService(
            onSearch = { throw IOException("Timed out connecting to marketzed.netlify.app") },
            onPricing = { throw IOException("Timed out connecting to marketzed.netlify.app") }
        )
        val repo = MarketplaceRepository(dao, failingApi, ConnectivityObserver(context))

        val retryResult = buildWorker(repo = repo, runAttemptCount = 0).doWork()
        assertEquals(ListenableWorker.Result.retry(), retryResult)

        val terminalResult = buildWorker(
            repo = repo,
            runAttemptCount = MarketplaceSyncWorker.MAX_RETRY_ATTEMPTS
        ).doWork()
        assertTrue(terminalResult is ListenableWorker.Result.Failure)

        // Non-retryable 4xx should fail immediately
        val badRequestApi = FakeMarketZedApiService(
            onSearch = {
                Response.error(400, "{\"error\":\"Invalid query\"}".toResponseBody("application/json".toMediaType()))
            },
            onPricing = {
                Response.error(400, "{\"error\":\"Bad request\"}".toResponseBody("application/json".toMediaType()))
            }
        )
        val repo400 = MarketplaceRepository(dao, badRequestApi, ConnectivityObserver(context))
        val clientErrorResult = buildWorker(repo = repo400, runAttemptCount = 0).doWork()
        assertTrue(clientErrorResult is ListenableWorker.Result.Failure)
    }

    @Test
    fun `scheduler configures unmetered periodic constraint for Wi-Fi only and updates when toggled`() {
        val fakeApi = FakeMarketZedApiService(
            onSearch = { Response.success(SearchResponseDto()) },
            onPricing = { Response.success(PromotionPricing()) }
        )
        val repo = MarketplaceRepository(dao, fakeApi, ConnectivityObserver(context))
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setWorkerFactory(MarketZedWorkerFactory(repo, dao))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        val workManager = WorkManager.getInstance(context)
        val scheduler = MarketplaceSyncScheduler(context)

        scheduler.schedulePeriodicSync(wifiOnly = true, dataSaverEnabled = true)
        val wifiOnlyInfos = workManager.getWorkInfosForUniqueWork(MarketplaceSyncScheduler.UNIQUE_PERIODIC_SYNC_NAME).get()
        assertEquals(1, wifiOnlyInfos.size)
        val wifiInfo = wifiOnlyInfos.first()
        assertEquals(WorkInfo.State.ENQUEUED, wifiInfo.state)
        assertEquals(NetworkType.UNMETERED, wifiInfo.constraints.requiredNetworkType)
        assertTrue(wifiInfo.constraints.requiresBatteryNotLow())

        scheduler.schedulePeriodicSync(wifiOnly = false, dataSaverEnabled = false)
        val anyNetInfos = workManager.getWorkInfosForUniqueWork(MarketplaceSyncScheduler.UNIQUE_PERIODIC_SYNC_NAME).get()
        assertEquals(1, anyNetInfos.size)
        assertEquals(NetworkType.CONNECTED, anyNetInfos.first().constraints.requiredNetworkType)

        scheduler.enqueueImmediateSync(force = true, respectWifiPref = false, source = "manual")
        val immediateInfos = workManager.getWorkInfosForUniqueWork(MarketplaceSyncScheduler.UNIQUE_IMMEDIATE_SYNC_NAME).get()
        assertEquals(1, immediateInfos.size)
    }

    private class FakeMarketZedApiService(
        private val onSearch: suspend (category: String?) -> Response<SearchResponseDto>,
        private val onPricing: suspend () -> Response<PromotionPricing>
    ) : MarketZedApiService {
        override suspend fun searchListings(
            query: String?,
            category: String?,
            subcategory: String?,
            province: String?,
            town: String?,
            condition: String?,
            sort: String?,
            minPrice: Double?,
            maxPrice: Double?,
            cursor: String?,
            limit: Int
        ): Response<SearchResponseDto> = onSearch(category)

        override suspend fun getPromotionPricing(): Response<PromotionPricing> = onPricing()

        override suspend fun submitTestimonial(body: TestimonialRequestDto): Response<Map<String, Any?>> =
            Response.success(emptyMap())

        override suspend fun getProfile(bearerToken: String): Response<UserProfile> =
            Response.success(UserProfile())

        override suspend fun updateProfile(
            bearerToken: String,
            body: ProfileUpdateRequestDto
        ): Response<UserProfile> = Response.success(UserProfile())

        override suspend fun getNotifications(bearerToken: String): Response<ItemsWrapperDto<AppNotification>> =
            Response.success(ItemsWrapperDto())

        override suspend fun markNotificationRead(
            bearerToken: String,
            body: MarkNotificationReadRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun uploadImage(
            bearerToken: String,
            file: MultipartBody.Part
        ): Response<CloudinaryUploadResponseDto> = Response.success(CloudinaryUploadResponseDto())

        override suspend fun submitListing(
            bearerToken: String,
            body: SubmitListingRequestDto
        ): Response<SubmitListingResponseDto> = Response.success(SubmitListingResponseDto())

        override suspend fun getMyListings(bearerToken: String): Response<ItemsWrapperDto<Listing>> =
            Response.success(ItemsWrapperDto())

        override suspend fun manageListing(
            bearerToken: String,
            body: ManageListingRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun reviseListing(
            bearerToken: String,
            body: ReviseListingRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun createPayment(
            bearerToken: String,
            body: CreatePaymentRequestDto
        ): Response<PaymentResponseDto> = Response.success(PaymentResponseDto())

        override suspend fun checkPaymentStatus(
            bearerToken: String,
            body: PaymentStatusRequestDto
        ): Response<PaymentResponseDto> = Response.success(PaymentResponseDto())

        override suspend fun getMyPayments(bearerToken: String): Response<ItemsWrapperDto<PaymentRecord>> =
            Response.success(ItemsWrapperDto())

        override suspend fun getMyPromotions(bearerToken: String): Response<ItemsWrapperDto<PromotionRecord>> =
            Response.success(ItemsWrapperDto())

        override suspend fun reportListing(
            bearerToken: String,
            body: ReportListingRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun getAdminOverview(bearerToken: String): Response<AdminOverview> =
            Response.success(AdminOverview())

        override suspend fun getAdminListings(
            bearerToken: String,
            status: String?,
            query: String?,
            limit: Int
        ): Response<ItemsWrapperDto<Listing>> = Response.success(ItemsWrapperDto())

        override suspend fun moderateAdminListing(
            bearerToken: String,
            body: AdminListingModerationRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun getAdminPromotions(
            bearerToken: String,
            status: String?
        ): Response<ItemsWrapperDto<AdminPromotionRecord>> = Response.success(ItemsWrapperDto())

        override suspend fun manageAdminPromotion(
            bearerToken: String,
            body: AdminPromotionManageRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun grantAdminPromotion(
            bearerToken: String,
            body: AdminGrantPromotionRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun updateAdminPromotionPricing(
            bearerToken: String,
            body: AdminUpdatePricingRequestDto
        ): Response<PromotionPricing> = Response.success(PromotionPricing())

        override suspend fun getAdminReports(
            bearerToken: String,
            status: String?
        ): Response<ItemsWrapperDto<AdminReportRecord>> = Response.success(ItemsWrapperDto())

        override suspend fun resolveAdminReport(
            bearerToken: String,
            body: AdminReportResolveRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun getAdminUsers(
            bearerToken: String,
            query: String?,
            filter: String?
        ): Response<ItemsWrapperDto<AdminUserRecord>> = Response.success(ItemsWrapperDto())

        override suspend fun manageAdminUser(
            bearerToken: String,
            body: AdminUserManageRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())

        override suspend fun getAdminPayments(
            bearerToken: String,
            status: String?
        ): Response<ItemsWrapperDto<AdminPaymentRecord>> = Response.success(ItemsWrapperDto())

        override suspend fun sendAdminBroadcast(
            bearerToken: String,
            body: AdminBroadcastRequestDto
        ): Response<Map<String, Any?>> = Response.success(emptyMap())
    }
}
