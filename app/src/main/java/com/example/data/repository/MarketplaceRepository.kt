package com.example.data.repository

import com.example.core.util.DateUtils
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.dev.DevFixturesManager
import com.example.data.local.CategoryCatalogueEntity
import com.example.data.local.LocationCatalogueEntity
import com.example.data.local.MarketZedDao
import com.example.data.local.SyncMetadataEntity
import com.example.data.model.CachedFeedResult
import com.example.data.model.CategoryOption
import com.example.data.model.Listing
import com.example.data.model.PromotionPricing
import com.example.data.model.ProvinceLocations
import com.example.data.model.SearchFilter
import com.example.data.model.SellerPublicProfile
import com.example.data.remote.ConnectivityObserver
import com.example.data.remote.MarketZedApiService
import com.example.data.remote.NetworkClient
import com.example.data.repository.LocalMappers.toCachedEntity
import com.example.data.repository.LocalMappers.toDomain
import com.squareup.moshi.Types
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

class HttpSyncException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

data class BackgroundSyncSummary(
    val wantedCount: Int = 0,
    val productsCount: Int = 0,
    val propertyCount: Int = 0,
    val swapsCount: Int = 0,
    val jobsCount: Int = 0,
    val servicesCount: Int = 0,
    val pricingSynced: Boolean = false,
    val prunedCount: Int = 0,
    val syncedAt: Long = System.currentTimeMillis(),
    val skippedReason: String? = null
) {
    val totalCachedCount: Int
        get() = productsCount + propertyCount + servicesCount + swapsCount + wantedCount + jobsCount
}

/**
 * Provides fair deterministic rotation across eligible active sponsored listings
 * without displaying the same sponsored item repetitively in adjacent slots.
 */
object SponsoredRotationHelper {
    fun filterEligibleSponsored(listings: List<Listing>): List<Listing> {
        return listings
            .asSequence()
            .filter { it.isPromoted && it.isEligiblePublic && !DateUtils.isExpired(it.expiresAt) }
            .distinctBy { it.id }
            .toList()
    }

    fun rotateFairly(
        listings: List<Listing>,
        rotationSlot: Int,
        excludeIds: Set<String> = emptySet(),
        limit: Int = 10
    ): List<Listing> {
        val eligible = filterEligibleSponsored(listings)
        if (eligible.isEmpty()) return emptyList()
        val nonExcluded = eligible.filterNot { it.id in excludeIds }
        val pool = if (nonExcluded.isNotEmpty()) nonExcluded else eligible
        val offset = ((rotationSlot % pool.size) + pool.size) % pool.size
        return (pool.drop(offset) + pool.take(offset)).take(limit)
    }

    fun prioritizeSponsoredInFeed(listings: List<Listing>): List<Listing> {
        val eligible = listings.filter { it.isEligiblePublic }.distinctBy { it.id }
        val sponsored = filterEligibleSponsored(eligible)
        val sponsoredIds = sponsored.map { it.id }.toSet()
        val organic = eligible.filterNot { it.id in sponsoredIds }
        return sponsored + organic
    }
}

class MarketplaceRepository(
    private val dao: MarketZedDao,
    private val api: MarketZedApiService,
    private val connectivityObserver: ConnectivityObserver,
    private val devFixturesManager: DevFixturesManager? = null
) {
    companion object {
        const val HOME_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        const val SEARCH_CACHE_TTL_MS = 3 * 60 * 1000L // 3 minutes
        const val CATALOGUE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val PROMO_PRICING_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours
        const val STALE_SEARCH_CACHE_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours

        const val SECTION_HOME_PRODUCTS = "home_products"
        const val SECTION_HOME_PROPERTY = "home_property"
        const val SECTION_HOME_SERVICES = "home_services"
        const val SECTION_HOME_SWAPS = "home_swaps"
        const val SECTION_HOME_WANTED = "home_wanted"
        const val SECTION_HOME_JOBS = "home_jobs"

        val HOME_SECTION_KEYS = listOf(
            SECTION_HOME_PRODUCTS,
            SECTION_HOME_PROPERTY,
            SECTION_HOME_SERVICES,
            SECTION_HOME_SWAPS,
            SECTION_HOME_WANTED,
            SECTION_HOME_JOBS
        )

        const val META_PROMOTION_PRICING = "promotion_pricing"
        const val META_WORK_SYNC = "workmanager_background_sync"
        private const val META_CATALOGUE_KEY = "catalogue_v2"
    }

    private val _inFlightSections = MutableStateFlow<Set<String>>(emptySet())
    val inFlightSections: StateFlow<Set<String>> = _inFlightSections.asStateFlow()

    private val stringListAdapter = NetworkClient.moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val promotionPricingAdapter = NetworkClient.moshi.adapter(PromotionPricing::class.java)

    suspend fun ensureCatalogueSeeded() {
        val now = System.currentTimeMillis()
        val existingMeta = dao.getSyncMetadata(META_CATALOGUE_KEY)
        if (existingMeta == null) {
            val catEntities = MarketZedCatalogue.categories.mapIndexed { index, cat ->
                CategoryCatalogueEntity(
                    id = cat.id,
                    name = cat.name,
                    description = cat.description,
                    subcategoriesJson = stringListAdapter.toJson(cat.subcategories),
                    requiresImage = cat.requiresImage,
                    sortOrder = index,
                    updatedAt = now
                )
            }
            val locEntities = MarketZedCatalogue.provinces.map { prov ->
                LocationCatalogueEntity(
                    province = prov.province,
                    townsJson = stringListAdapter.toJson(prov.towns),
                    updatedAt = now
                )
            }
            dao.upsertCategories(catEntities)
            dao.upsertLocations(locEntities)
            dao.upsertSyncMetadata(
                SyncMetadataEntity(
                    cacheKey = META_CATALOGUE_KEY,
                    lastSyncedAt = now,
                    ttlMillis = CATALOGUE_TTL_MS,
                    nextCursor = null,
                    lastError = null
                )
            )
        }
    }

    fun observeCategories(): Flow<List<CategoryOption>> =
        dao.observeCategories().map { rows ->
            if (rows.isEmpty()) {
                MarketZedCatalogue.categories
            } else {
                rows.map { row ->
                    CategoryOption(
                        id = row.id,
                        name = row.name,
                        description = row.description,
                        subcategories = runCatching {
                            stringListAdapter.fromJson(row.subcategoriesJson)
                        }.getOrNull().orEmpty(),
                        requiresImage = row.requiresImage
                    )
                }
            }
        }

    fun observeLocations(): Flow<List<ProvinceLocations>> =
        dao.observeLocations().map { rows ->
            if (rows.isEmpty()) {
                MarketZedCatalogue.provinces
            } else {
                rows.map { row ->
                    ProvinceLocations(
                        province = row.province,
                        towns = runCatching {
                            stringListAdapter.fromJson(row.townsJson)
                        }.getOrNull().orEmpty()
                    )
                }
            }
        }

    fun observeSectionFeed(sectionKey: String): Flow<CachedFeedResult<Listing>> {
        val devKey = DevFixturesManager.devSectionKey(sectionKey)
        val allowDev = devFixturesManager?.isFeatureAvailable == true
        val sectionsFlow = if (allowDev) {
            dao.observeListingsBySections(listOf(sectionKey, devKey))
        } else {
            dao.observeListingsBySection(sectionKey)
        }
        val devSeededFlow: Flow<Boolean> = devFixturesManager?.isSeeded ?: MutableStateFlow(false)
        return combine(
            sectionsFlow,
            dao.observeSyncMetadata(sectionKey),
            connectivityObserver.isConnectedHint,
            devSeededFlow
        ) { rows, meta, connectedHint, devSeeded ->
            val includeDev = allowDev && devSeeded
            val items = rows
                .asSequence()
                .filter { row ->
                    if (row.sectionKey.startsWith(DevFixturesManager.DEV_PREFIX)) includeDev else true
                }
                .map { it.toDomain() }
                .filter { it.isEligiblePublic }
                .distinctBy { it.id }
                .toList()
            val now = System.currentTimeMillis()
            val lastSync = meta?.lastSyncedAt
            val ttl = meta?.ttlMillis ?: HOME_CACHE_TTL_MS
            val isStale = lastSync == null || (now - lastSync) > ttl
            CachedFeedResult(
                items = items,
                isStale = isStale,
                isOffline = !connectedHint,
                lastSyncedAt = lastSync,
                nextCursor = meta?.nextCursor,
                errorMessage = meta?.lastError
            )
        }
    }

    fun observeListingById(listingId: String): Flow<Listing?> =
        dao.observeListingById(listingId).map { it?.toDomain() }

    suspend fun getCachedListingById(listingId: String): Listing? =
        dao.getListingById(listingId)?.toDomain()

    /**
     * Targeted lookup for comparison IDs without downloading entire collections.
     * Checks local Room first and falls back to bounded lookup for missing IDs when online.
     */
    suspend fun getListingsForComparison(ids: List<String>): Map<String, Listing?> {
        val cleanIds = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(3)
        if (cleanIds.isEmpty()) return emptyMap()
        val cachedRows = dao.getListingsByIds(cleanIds)
            .map { it.toDomain() }
            .distinctBy { it.id }
            .associateBy { it.id }

        val result = linkedMapOf<String, Listing?>()
        val missingIds = mutableListOf<String>()
        cleanIds.forEach { id ->
            val hit = cachedRows[id]
            if (hit != null) {
                result[id] = hit
            } else {
                missingIds += id
            }
        }

        if (missingIds.isNotEmpty() && connectivityObserver.checkCurrentlyConnected()) {
            // Bounded small fallback search query by recent items so missing comparison IDs resolve if live
            runCatching {
                val resp = api.searchListings(limit = 30)
                if (resp.isSuccessful) {
                    val now = System.currentTimeMillis()
                    val found = resp.body()?.items.orEmpty().filter { it.id in missingIds }
                    if (found.isNotEmpty()) {
                        dao.upsertCachedListings(found.map { it.toCachedEntity("detail_snapshot", now) })
                    }
                    found.forEach { result[it.id] = it }
                }
            }
        }
        cleanIds.forEach { id ->
            if (!result.containsKey(id)) {
                result[id] = null
            }
        }
        return result
    }

    fun observeSellerListings(sellerKey: String): Flow<List<Listing>> {
        val cleanKey = sellerKey.trim()
        val includeDev = devFixturesManager?.let { it.isFeatureAvailable && it.isSeeded.value } == true
        return dao.observeAllCachedListings().map { rows ->
            rows.asSequence()
                .filter { row ->
                    if (row.sectionKey.startsWith(DevFixturesManager.DEV_PREFIX)) includeDev else true
                }
                .map { it.toDomain() }
                .distinctBy { it.id }
                .filter { it.isEligiblePublic }
                .filter { listing ->
                    (!listing.ownerId.isNullOrBlank() && listing.ownerId.equals(cleanKey, ignoreCase = true)) ||
                        (!listing.sellerName.isNullOrBlank() && listing.sellerName.equals(cleanKey, ignoreCase = true))
                }
                .sortedByDescending { it.createdAtMillis }
                .toList()
        }
    }

    fun buildSellerPublicProfile(
        sellerKey: String,
        listings: List<Listing>,
        anchorListing: Listing? = null
    ): SellerPublicProfile? {
        val allMatches = (listOfNotNull(anchorListing) + listings).distinctBy { it.id }
        val primary = anchorListing ?: allMatches.firstOrNull() ?: return null
        val activeListings = allMatches.filter { it.isEligiblePublic }
        val rawAccountType = primary.sellerAccountType?.trim()?.lowercase()
        val resolvedAccountType = if (rawAccountType == "business") "Business" else "Personal"
        return SellerPublicProfile(
            sellerKey = sellerKey,
            ownerId = primary.ownerId,
            displayName = primary.sellerName?.trim().takeIf { !it.isNullOrBlank() } ?: "MarketZed Seller",
            accountType = resolvedAccountType,
            verified = allMatches.any { it.sellerVerified },
            photoUrl = allMatches.firstNotNullOfOrNull { it.sellerPhotoUrl?.takeIf { p -> p.isNotBlank() } },
            bio = allMatches.firstNotNullOfOrNull { it.sellerBio?.takeIf { b -> b.isNotBlank() } },
            memberSince = allMatches.firstNotNullOfOrNull { it.sellerMemberSince?.takeIf { m -> m.isNotBlank() } },
            primaryLocation = primary.location?.displayFull?.takeIf { it != "Location not specified" },
            contactPreferences = allMatches.firstNotNullOfOrNull { it.contactPreferences?.takeIf { c -> c.hasAny } },
            listings = activeListings
        )
    }

    /**
     * Independent concurrent loading in MarketZed priority order:
     * 1. Products, 2. Rental properties, 3. Services, 4. Swaps, 5. Wanted, 6. Jobs.
     * Each section persists into Room immediately as its own network call completes.
     */
    suspend fun refreshHomeFeeds(force: Boolean = false): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            if (!force) {
                val productsMeta = dao.getSyncMetadata(SECTION_HOME_PRODUCTS)
                val propertyMeta = dao.getSyncMetadata(SECTION_HOME_PROPERTY)
                if (productsMeta != null && propertyMeta != null && (now - productsMeta.lastSyncedAt) < HOME_CACHE_TTL_MS) {
                    return Result.success(Unit)
                }
            }

            val jobsSpec = listOf(
                Triple(SECTION_HOME_PRODUCTS, SearchFilter(category = "marketplace", sort = "newest"), 24),
                Triple(SECTION_HOME_PROPERTY, SearchFilter(category = "property", sort = "newest"), 14),
                Triple(SECTION_HOME_SERVICES, SearchFilter(category = "services", sort = "newest"), 10),
                Triple(SECTION_HOME_SWAPS, SearchFilter(category = "swaps", sort = "newest"), 10),
                Triple(SECTION_HOME_WANTED, SearchFilter(category = "wanted", sort = "newest"), 8),
                Triple(SECTION_HOME_JOBS, SearchFilter(category = "jobs", sort = "newest"), 10)
            )

            val results = coroutineScope {
                jobsSpec.map { (sectionKey, filter, limit) ->
                    async {
                        fetchAndCacheSection(
                            sectionKey = sectionKey,
                            filter = filter,
                            limit = limit
                        )
                    }
                }.awaitAll()
            }

            if (results.all { it.isFailure }) {
                Result.failure(results.first().exceptionOrNull() ?: IOException("Unable to reach MarketZed"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchAndCacheSection(
        sectionKey: String,
        filter: SearchFilter,
        limit: Int
    ): Result<List<Listing>> {
        _inFlightSections.update { it + sectionKey }
        return try {
            val response = api.searchListings(
                query = filter.query.takeIf { it.isNotBlank() },
                category = filter.category,
                subcategory = filter.subcategory,
                province = filter.province,
                town = filter.town,
                condition = filter.condition,
                sort = filter.sort,
                minPrice = filter.minPrice,
                maxPrice = filter.maxPrice,
                cursor = null,
                limit = limit.coerceIn(1, 40)
            )
            val now = System.currentTimeMillis()
            if (response.isSuccessful) {
                val body = response.body()
                val eligible = body?.items.orEmpty().filter { it.isEligiblePublic }
                dao.replaceSectionListings(
                    sectionKey = sectionKey,
                    listings = eligible.map { it.toCachedEntity(sectionKey, now) }
                )
                dao.upsertSyncMetadata(
                    SyncMetadataEntity(
                        cacheKey = sectionKey,
                        lastSyncedAt = now,
                        ttlMillis = HOME_CACHE_TTL_MS,
                        nextCursor = body?.nextCursor,
                        lastError = null
                    )
                )
                Result.success(eligible)
            } else {
                val err = NetworkClient.parseErrorMessage(response, "Unable to refresh feed (${response.code()})")
                updateSyncError(sectionKey, err)
                Result.failure(HttpSyncException(response.code(), err))
            }
        } catch (e: Exception) {
            val err = if (e is IOException) "Offline — showing cached listings" else (e.localizedMessage ?: "Network error")
            updateSyncError(sectionKey, err)
            Result.failure(e)
        } finally {
            _inFlightSections.update { it - sectionKey }
        }
    }

    fun observeBackgroundSyncMetadata(): Flow<SyncMetadataEntity?> =
        dao.observeSyncMetadata(META_WORK_SYNC)

    suspend fun syncPromotionPricing(): Result<PromotionPricing> {
        return try {
            val response = api.getPromotionPricing()
            val now = System.currentTimeMillis()
            if (response.isSuccessful) {
                val pricing = response.body() ?: PromotionPricing()
                dao.upsertSyncMetadata(
                    SyncMetadataEntity(
                        cacheKey = META_PROMOTION_PRICING,
                        lastSyncedAt = now,
                        ttlMillis = PROMO_PRICING_TTL_MS,
                        nextCursor = promotionPricingAdapter.toJson(pricing),
                        lastError = null
                    )
                )
                Result.success(pricing)
            } else {
                val err = NetworkClient.parseErrorMessage(
                    response,
                    "Unable to sync promotion pricing (${response.code()})"
                )
                updateSyncError(META_PROMOTION_PRICING, err, PROMO_PRICING_TTL_MS)
                Result.failure(HttpSyncException(response.code(), err))
            }
        } catch (e: Exception) {
            val err = if (e is IOException) "Offline — using cached pricing" else (e.localizedMessage ?: "Network error")
            updateSyncError(META_PROMOTION_PRICING, err, PROMO_PRICING_TTL_MS)
            Result.failure(e)
        }
    }

    suspend fun getCachedPromotionPricing(): PromotionPricing? {
        val raw = dao.getSyncMetadata(META_PROMOTION_PRICING)?.nextCursor ?: return null
        return runCatching { promotionPricingAdapter.fromJson(raw) }.getOrNull()
    }

    suspend fun syncBackgroundSnapshot(
        force: Boolean = false,
        pruneOlderThanMs: Long = STALE_SEARCH_CACHE_AGE_MS
    ): Result<BackgroundSyncSummary> {
        return try {
            ensureCatalogueSeeded()
            val (feedsResult, pricingResult) = coroutineScope {
                val feedsDeferred = async { refreshHomeFeeds(force = force) }
                val pricingDeferred = async { syncPromotionPricing() }
                feedsDeferred.await() to pricingDeferred.await()
            }

            if (feedsResult.isFailure && pricingResult.isFailure) {
                val error = feedsResult.exceptionOrNull()
                    ?: pricingResult.exceptionOrNull()
                    ?: IOException("Unable to synchronize with MarketZed")
                updateSyncError(META_WORK_SYNC, error.localizedMessage ?: "Background sync failed")
                return Result.failure(error)
            }

            val now = System.currentTimeMillis()
            val productsCount = dao.countSectionListings(SECTION_HOME_PRODUCTS)
            val propertyCount = dao.countSectionListings(SECTION_HOME_PROPERTY)
            val servicesCount = dao.countSectionListings(SECTION_HOME_SERVICES)
            val swapsCount = dao.countSectionListings(SECTION_HOME_SWAPS)
            val wantedCount = dao.countSectionListings(SECTION_HOME_WANTED)
            val jobsCount = dao.countSectionListings(SECTION_HOME_JOBS)
            val prunedCount = dao.pruneStaleSearchCache(now - pruneOlderThanMs)
            val summary = BackgroundSyncSummary(
                wantedCount = wantedCount,
                productsCount = productsCount,
                propertyCount = propertyCount,
                swapsCount = swapsCount,
                jobsCount = jobsCount,
                servicesCount = servicesCount,
                pricingSynced = pricingResult.isSuccess,
                prunedCount = prunedCount,
                syncedAt = now
            )

            val partialWarning = feedsResult.exceptionOrNull()?.localizedMessage
                ?: pricingResult.exceptionOrNull()?.localizedMessage
            val summaryText = buildString {
                append("${summary.totalCachedCount} cached listings")
                if (summary.pricingSynced) append(" • promo pricing synced")
                if (summary.prunedCount > 0) append(" • ${summary.prunedCount} stale pruned")
            }

            dao.upsertSyncMetadata(
                SyncMetadataEntity(
                    cacheKey = META_WORK_SYNC,
                    lastSyncedAt = now,
                    ttlMillis = HOME_CACHE_TTL_MS,
                    nextCursor = summaryText,
                    lastError = partialWarning
                )
            )
            Result.success(summary)
        } catch (e: Exception) {
            updateSyncError(META_WORK_SYNC, e.localizedMessage ?: "Background sync failed")
            Result.failure(e)
        }
    }

    suspend fun searchListings(
        filter: SearchFilter,
        cursor: String? = null,
        limit: Int = 20
    ): Result<CachedFeedResult<Listing>> {
        val sectionKey = "search:${filter.cacheKey()}"
        val now = System.currentTimeMillis()
        return try {
            val response = api.searchListings(
                query = filter.query.trim().takeIf { it.isNotBlank() },
                category = filter.category?.takeIf { it.isNotBlank() },
                subcategory = filter.subcategory?.takeIf { it.isNotBlank() },
                province = filter.province?.takeIf { it.isNotBlank() },
                town = filter.town?.takeIf { it.isNotBlank() },
                condition = filter.condition?.takeIf { it.isNotBlank() },
                sort = filter.sort,
                minPrice = filter.minPrice,
                maxPrice = filter.maxPrice,
                cursor = cursor,
                limit = limit.coerceIn(1, 40)
            )
            if (response.isSuccessful) {
                val body = response.body()
                val filtered = body?.items.orEmpty()
                    .filter { it.isEligiblePublic }
                    .filter { matchesClientConstraints(it, filter) }
                val prioritized = if (devFixturesManager?.isFeatureAvailable == true && devFixturesManager.isSeeded.value && cursor == null) {
                    val devMatches = searchDevFixturesOnly(filter)
                    SponsoredRotationHelper.prioritizeSponsoredInFeed((filtered + devMatches).distinctBy { it.id })
                } else {
                    SponsoredRotationHelper.prioritizeSponsoredInFeed(filtered)
                }

                if (cursor == null) {
                    dao.replaceSectionListings(
                        sectionKey = sectionKey,
                        listings = prioritized
                            .filterNot { it.id.startsWith("dev_") }
                            .map { it.toCachedEntity(sectionKey, now) }
                    )
                    dao.upsertSyncMetadata(
                        SyncMetadataEntity(
                            cacheKey = sectionKey,
                            lastSyncedAt = now,
                            ttlMillis = SEARCH_CACHE_TTL_MS,
                            nextCursor = body?.nextCursor,
                            lastError = null
                        )
                    )
                } else if (prioritized.isNotEmpty()) {
                    dao.upsertCachedListings(prioritized.map { it.toCachedEntity(sectionKey, now) })
                }
                Result.success(
                    CachedFeedResult(
                        items = prioritized,
                        isStale = false,
                        isOffline = false,
                        lastSyncedAt = now,
                        nextCursor = body?.nextCursor
                    )
                )
            } else {
                val apiMsg = NetworkClient.parseErrorMessage(
                    response,
                    "Search failed (${response.code()})"
                )
                val friendlyMsg = if (response.code() == 400 &&
                    (apiMsg.contains("keyword", ignoreCase = true) || apiMsg.contains("token", ignoreCase = true) || filter.query.trim().contains(" "))
                ) {
                    "Search currently works best with one keyword. Try a shorter search."
                } else {
                    apiMsg
                }
                Result.failure(Exception(friendlyMsg))
            }
        } catch (e: Exception) {
            if (cursor == null) {
                val meta = dao.getSyncMetadata(sectionKey)
                val fallbackList = searchCachedOfflineSnapshot(filter)
                if (fallbackList.isNotEmpty() || meta != null) {
                    return Result.success(
                        CachedFeedResult(
                            items = fallbackList,
                            isStale = true,
                            isOffline = true,
                            lastSyncedAt = meta?.lastSyncedAt,
                            nextCursor = null,
                            errorMessage = "Offline — showing cached results"
                        )
                    )
                }
            }
            val msg = if (e is IOException) {
                "You appear to be offline. Connect to search live MarketZed listings."
            } else {
                e.localizedMessage ?: "Search error"
            }
            Result.failure(Exception(msg))
        }
    }

    private suspend fun searchDevFixturesOnly(filter: SearchFilter): List<Listing> {
        if (devFixturesManager?.isFeatureAvailable != true || !devFixturesManager.isSeeded.value) {
            return emptyList()
        }
        val q = filter.query.trim().lowercase()
        return dao.getAllCachedListingsSnapshot()
            .asSequence()
            .filter { it.sectionKey.startsWith(DevFixturesManager.DEV_PREFIX) }
            .map { it.toDomain() }
            .distinctBy { it.id }
            .filter { it.isEligiblePublic }
            .filter { item ->
                (filter.category.isNullOrBlank() || item.categoryId.equals(filter.category, ignoreCase = true)) &&
                    (filter.subcategory.isNullOrBlank() || item.subcategory.equals(filter.subcategory, ignoreCase = true)) &&
                    (filter.province.isNullOrBlank() || item.location?.province.equals(filter.province, ignoreCase = true)) &&
                    matchesClientConstraints(item, filter) &&
                    (q.isBlank() ||
                        item.title.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.location?.town?.lowercase()?.contains(q) == true ||
                        item.location?.district?.lowercase()?.contains(q) == true ||
                        item.location?.nearbyArea?.lowercase()?.contains(q) == true)
            }
            .sortedWith(
                if (filter.sort == "oldest") compareBy { it.createdAtMillis }
                else compareByDescending { it.createdAtMillis }
            )
            .toList()
    }

    private fun matchesClientConstraints(item: Listing, filter: SearchFilter): Boolean {
        val districtOk = filter.district.isNullOrBlank() ||
            item.location?.district?.equals(filter.district, ignoreCase = true) == true ||
            item.location?.town?.equals(filter.district, ignoreCase = true) == true
        val townOk = filter.town.isNullOrBlank() ||
            item.location?.town?.equals(filter.town, ignoreCase = true) == true
        val nearbyQuery = filter.nearbyArea?.trim()?.lowercase().orEmpty()
        val nearbyOk = nearbyQuery.isBlank() || listOfNotNull(
            item.location?.nearbyArea,
            item.location?.town,
            item.location?.district,
            item.location?.province,
            item.description
        ).any { it.lowercase().contains(nearbyQuery) }
        val condOk = filter.condition.isNullOrBlank() ||
            item.categoryDetails?.condition?.equals(filter.condition, ignoreCase = true) == true ||
            item.resolvedSpecifications["Condition"]?.equals(filter.condition, ignoreCase = true) == true
        val minOk = filter.minPrice == null || (item.price != null && item.price >= filter.minPrice)
        val maxOk = filter.maxPrice == null || (item.price != null && item.price <= filter.maxPrice)
        return districtOk && townOk && nearbyOk && condOk && minOk && maxOk
    }

    suspend fun searchCachedOfflineSnapshot(filter: SearchFilter): List<Listing> {
        val q = filter.query.trim().lowercase()
        val includeDev = devFixturesManager?.let { it.isFeatureAvailable && it.isSeeded.value } == true
        val matched = dao.getAllCachedListingsSnapshot()
            .asSequence()
            .filter { row ->
                if (row.sectionKey.startsWith(DevFixturesManager.DEV_PREFIX)) includeDev else true
            }
            .map { it.toDomain() }
            .distinctBy { it.id }
            .filter { it.isEligiblePublic }
            .filter { item ->
                (filter.category.isNullOrBlank() || item.categoryId.equals(filter.category, ignoreCase = true)) &&
                    (filter.subcategory.isNullOrBlank() || item.subcategory.equals(filter.subcategory, ignoreCase = true)) &&
                    (filter.province.isNullOrBlank() || item.location?.province.equals(filter.province, ignoreCase = true)) &&
                    matchesClientConstraints(item, filter) &&
                    (q.isBlank() ||
                        item.title.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.location?.town?.lowercase()?.contains(q) == true ||
                        item.location?.district?.lowercase()?.contains(q) == true ||
                        item.location?.nearbyArea?.lowercase()?.contains(q) == true)
            }
            .sortedWith(
                if (filter.sort == "oldest") compareBy { it.createdAtMillis }
                else compareByDescending { it.createdAtMillis }
            )
            .toList()
        return SponsoredRotationHelper.prioritizeSponsoredInFeed(matched)
    }

    suspend fun upsertListingSnapshot(listing: Listing, sectionKey: String = "detail_snapshot") {
        dao.upsertCachedListings(listOf(listing.toCachedEntity(sectionKey)))
    }

    suspend fun clearPublicCachePreservingDrafts() {
        dao.clearAllPublicListingCache()
        dao.clearSyncMetadata()
    }

    private suspend fun updateSyncError(
        sectionKey: String,
        err: String,
        defaultTtlMillis: Long = HOME_CACHE_TTL_MS
    ) {
        val prev = dao.getSyncMetadata(sectionKey)
        dao.upsertSyncMetadata(
            SyncMetadataEntity(
                cacheKey = sectionKey,
                lastSyncedAt = prev?.lastSyncedAt ?: 0L,
                ttlMillis = prev?.ttlMillis ?: defaultTtlMillis,
                nextCursor = prev?.nextCursor,
                lastError = err
            )
        )
    }
}
