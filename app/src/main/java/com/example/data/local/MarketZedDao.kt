package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketZedDao {

    // ---- Cached Public Listings ----
    @Query("SELECT * FROM cached_listings WHERE sectionKey = :sectionKey ORDER BY isPromoted DESC, cachedAt DESC")
    fun observeListingsBySection(sectionKey: String): Flow<List<CachedListingEntity>>

    @Query("SELECT * FROM cached_listings WHERE id = :listingId LIMIT 1")
    fun observeListingById(listingId: String): Flow<CachedListingEntity?>

    @Query("SELECT * FROM cached_listings WHERE id = :listingId LIMIT 1")
    suspend fun getListingById(listingId: String): CachedListingEntity?

    @Query("SELECT * FROM cached_listings WHERE id IN (:listingIds)")
    suspend fun getListingsByIds(listingIds: List<String>): List<CachedListingEntity>

    @Query("SELECT * FROM cached_listings ORDER BY cachedAt DESC")
    fun observeAllCachedListings(): Flow<List<CachedListingEntity>>

    @Query("SELECT * FROM cached_listings ORDER BY cachedAt DESC")
    suspend fun getAllCachedListingsSnapshot(): List<CachedListingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedListings(listings: List<CachedListingEntity>)

    @Query("DELETE FROM cached_listings WHERE sectionKey = :sectionKey")
    suspend fun deleteSectionListings(sectionKey: String)

    @Transaction
    suspend fun replaceSectionListings(sectionKey: String, listings: List<CachedListingEntity>) {
        deleteSectionListings(sectionKey)
        if (listings.isNotEmpty()) {
            upsertCachedListings(listings)
        }
    }

    @Query("SELECT COUNT(*) FROM cached_listings WHERE sectionKey = :sectionKey")
    suspend fun countSectionListings(sectionKey: String): Int

    @Query(
        """
        DELETE FROM cached_listings
        WHERE sectionKey LIKE 'search:%'
          AND cachedAt < :cutoffMillis
          AND id NOT IN (SELECT listingId FROM favorites)
          AND id NOT IN (SELECT listingId FROM recently_viewed)
          AND id NOT IN (SELECT listingId FROM comparison_selections)
        """
    )
    suspend fun pruneStaleSearchCache(cutoffMillis: Long): Int

    @Query("DELETE FROM cached_listings WHERE sectionKey LIKE 'dev_fixture_%'")
    suspend fun deleteDevFixtureListings(): Int

    @Query("SELECT * FROM cached_listings WHERE sectionKey IN (:sectionKeys) ORDER BY isPromoted DESC, cachedAt DESC")
    fun observeListingsBySections(sectionKeys: List<String>): Flow<List<CachedListingEntity>>

    @Query("DELETE FROM cached_listings")
    suspend fun clearAllPublicListingCache()

    // ---- Catalogue ----
    @Query("SELECT * FROM category_catalogue ORDER BY sortOrder ASC")
    fun observeCategories(): Flow<List<CategoryCatalogueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CategoryCatalogueEntity>)

    @Query("SELECT * FROM location_catalogue ORDER BY province ASC")
    fun observeLocations(): Flow<List<LocationCatalogueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(items: List<LocationCatalogueEntity>)

    // ---- Favorites (Account-scoped) ----
    @Query("SELECT listingId FROM favorites WHERE userScope = :userScope ORDER BY savedAt DESC")
    fun observeFavoriteIds(userScope: String): Flow<List<String>>

    @Query(
        """
        SELECT cl.* FROM cached_listings cl
        INNER JOIN favorites f ON cl.id = f.listingId
        WHERE f.userScope = :userScope
        GROUP BY cl.id
        ORDER BY MAX(f.savedAt) DESC
        """
    )
    fun observeFavoriteListings(userScope: String): Flow<List<CachedListingEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userScope = :userScope AND listingId = :listingId)")
    suspend fun isFavorite(userScope: String, listingId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userScope = :userScope AND listingId = :listingId")
    suspend fun deleteFavorite(userScope: String, listingId: String)

    @Transaction
    suspend fun toggleFavorite(userScope: String, listingId: String): Boolean {
        val exists = isFavorite(userScope, listingId)
        if (exists) {
            deleteFavorite(userScope, listingId)
        } else {
            insertFavorite(FavoriteEntity(userScope = userScope, listingId = listingId, savedAt = System.currentTimeMillis()))
        }
        return !exists
    }

    // ---- Recently Viewed (Account-scoped, bounded to 40) ----
    @Query(
        """
        SELECT cl.* FROM cached_listings cl
        INNER JOIN recently_viewed rv ON cl.id = rv.listingId
        WHERE rv.userScope = :userScope
        GROUP BY cl.id
        ORDER BY MAX(rv.viewedAt) DESC
        LIMIT 40
        """
    )
    fun observeRecentlyViewed(userScope: String): Flow<List<CachedListingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentlyViewed(entry: RecentlyViewedEntity)

    @Query(
        """
        DELETE FROM recently_viewed
        WHERE userScope = :userScope AND listingId NOT IN (
            SELECT listingId FROM recently_viewed WHERE userScope = :userScope ORDER BY viewedAt DESC LIMIT 40
        )
        """
    )
    suspend fun trimRecentlyViewed(userScope: String)

    @Transaction
    suspend fun recordRecentlyViewed(userScope: String, listingId: String) {
        upsertRecentlyViewed(RecentlyViewedEntity(userScope = userScope, listingId = listingId, viewedAt = System.currentTimeMillis()))
        trimRecentlyViewed(userScope)
    }

    @Query("DELETE FROM recently_viewed WHERE userScope = :userScope")
    suspend fun clearRecentlyViewed(userScope: String)

    // ---- Local Drafts (Account-scoped, device-only) ----
    @Query("SELECT * FROM listing_drafts WHERE userScope = :userScope ORDER BY updatedAt DESC")
    fun observeDrafts(userScope: String): Flow<List<ListingDraftEntity>>

    @Query("SELECT * FROM listing_drafts ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentDraft(): ListingDraftEntity?

    @Query("SELECT * FROM listing_drafts WHERE localUuid = :localUuid LIMIT 1")
    suspend fun getDraft(localUuid: String): ListingDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: ListingDraftEntity)

    @Query("DELETE FROM listing_drafts WHERE localUuid = :localUuid")
    suspend fun deleteDraft(localUuid: String)

    // ---- Search History (Account-scoped) ----
    @Query("SELECT * FROM search_history WHERE userScope = :userScope ORDER BY searchedAt DESC LIMIT 12")
    fun observeSearchHistory(userScope: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE userScope = :userScope AND query = :query")
    suspend fun deleteSearchHistoryItem(userScope: String, query: String)

    @Query("DELETE FROM search_history WHERE userScope = :userScope")
    suspend fun clearSearchHistory(userScope: String)

    // ---- Saved Searches (Account-scoped) ----
    @Query("SELECT * FROM saved_searches WHERE userScope = :userScope ORDER BY createdAt DESC")
    fun observeSavedSearches(userScope: String): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches WHERE notifyMatches = 1 ORDER BY createdAt DESC")
    suspend fun getAllAlertSavedSearches(): List<SavedSearchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedSearch(search: SavedSearchEntity)

    @Query("UPDATE saved_searches SET notifyMatches = :enabled WHERE id = :id AND userScope = :userScope")
    suspend fun updateSavedSearchAlert(userScope: String, id: String, enabled: Boolean)

    @Query("UPDATE saved_searches SET lastMatchedAt = :matchedAt WHERE id = :id")
    suspend fun updateSavedSearchLastMatched(id: String, matchedAt: Long)

    @Query("DELETE FROM saved_searches WHERE id = :id AND userScope = :userScope")
    suspend fun deleteSavedSearch(userScope: String, id: String)

    // ---- Product Comparison Selection (Account-scoped, Max 3) ----
    @Query("SELECT listingId FROM comparison_selections WHERE userScope = :userScope ORDER BY addedAt ASC")
    fun observeComparisonIds(userScope: String): Flow<List<String>>

    @Query("SELECT listingId FROM comparison_selections WHERE userScope = :userScope ORDER BY addedAt ASC")
    suspend fun getComparisonIds(userScope: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComparisonSelection(selection: ComparisonSelectionEntity)

    @Query("DELETE FROM comparison_selections WHERE userScope = :userScope AND listingId = :listingId")
    suspend fun removeComparisonSelection(userScope: String, listingId: String)

    @Query("DELETE FROM comparison_selections WHERE userScope = :userScope")
    suspend fun clearComparisonSelections(userScope: String)

    @Transaction
    suspend fun addComparisonSelectionUpToThree(userScope: String, listingId: String): Int {
        val existing = getComparisonIds(userScope)
        if (existing.contains(listingId)) return 0 // Already selected
        if (existing.size >= 3) return -1 // Limit reached
        insertComparisonSelection(
            ComparisonSelectionEntity(
                userScope = userScope,
                listingId = listingId,
                addedAt = System.currentTimeMillis()
            )
        )
        return 1 // Added
    }

    // ---- Sync Metadata ----
    @Query("SELECT * FROM sync_metadata WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeSyncMetadata(cacheKey: String): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getSyncMetadata(cacheKey: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMetadata(meta: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearSyncMetadata()

    // ---- App Settings ----
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
