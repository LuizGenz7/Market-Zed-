package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_listings",
    primaryKeys = ["sectionKey", "id"],
    indices = [
        Index(value = ["id"]),
        Index(value = ["sectionKey"]),
        Index(value = ["categoryId"]),
        Index(value = ["cachedAt"])
    ]
)
data class CachedListingEntity(
    val id: String,
    val sectionKey: String,
    val ownerId: String?,
    val sellerName: String?,
    val sellerVerified: Boolean,
    val isPromoted: Boolean,
    val categoryId: String,
    val subcategory: String,
    val title: String,
    val description: String,
    val price: Double?,
    val currency: String,
    val priceMode: String?,
    val province: String,
    val town: String,
    val imagesJson: String,
    val contactPreferencesJson: String?,
    val categoryDetailsJson: String?,
    val moderationStatus: String?,
    val availabilityStatus: String?,
    val wantedStatus: String?,
    val isPublic: Boolean,
    val reviewRequired: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
    val publishedAt: String?,
    val expiresAt: String?,
    val cachedAt: Long,
    val originalPrice: Double? = null,
    val sellerPhotoUrl: String? = null,
    val sellerAccountType: String? = null,
    val sellerBio: String? = null,
    val sellerMemberSince: String? = null,
    val specificationsJson: String? = null,
    val campaignFormat: String? = null,
    val impressionsCount: Int? = null,
    val clicksCount: Int? = null,
    val visitsCount: Int? = null
)

@Entity(tableName = "category_catalogue")
data class CategoryCatalogueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val subcategoriesJson: String,
    val requiresImage: Boolean,
    val sortOrder: Int,
    val updatedAt: Long
)

@Entity(tableName = "location_catalogue")
data class LocationCatalogueEntity(
    @PrimaryKey val province: String,
    val townsJson: String,
    val updatedAt: Long
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["userScope", "listingId"],
    indices = [Index(value = ["userScope", "savedAt"])]
)
data class FavoriteEntity(
    val userScope: String,
    val listingId: String,
    val savedAt: Long
)

@Entity(
    tableName = "recently_viewed",
    primaryKeys = ["userScope", "listingId"],
    indices = [Index(value = ["userScope", "viewedAt"])]
)
data class RecentlyViewedEntity(
    val userScope: String,
    val listingId: String,
    val viewedAt: Long
)

@Entity(
    tableName = "listing_drafts",
    indices = [Index(value = ["userScope", "updatedAt"])]
)
data class ListingDraftEntity(
    @PrimaryKey val localUuid: String,
    val userScope: String,
    val step: Int,
    val categoryId: String,
    val subcategory: String,
    val title: String,
    val description: String,
    val price: String,
    val province: String,
    val town: String,
    val categoryDetailsJson: String,
    val contactPreferencesJson: String,
    val localImagePathsJson: String,
    val uploadedImagesJson: String,
    val ownershipDeclarationAccepted: Boolean,
    val ownershipDeclarationVersion: String,
    val updatedAt: Long
)

@Entity(
    tableName = "search_history",
    primaryKeys = ["userScope", "query"],
    indices = [Index(value = ["userScope", "searchedAt"])]
)
data class SearchHistoryEntity(
    val userScope: String,
    val query: String,
    val searchedAt: Long
)

@Entity(
    tableName = "saved_searches",
    indices = [Index(value = ["userScope", "createdAt"])]
)
data class SavedSearchEntity(
    @PrimaryKey val id: String,
    val userScope: String,
    val name: String,
    val query: String,
    val category: String?,
    val subcategory: String?,
    val province: String?,
    val town: String?,
    val minPrice: Double?,
    val maxPrice: Double?,
    val condition: String?,
    val sort: String,
    val notifyMatches: Boolean,
    val createdAt: Long,
    val lastMatchedAt: Long?
)

@Entity(
    tableName = "comparison_selections",
    primaryKeys = ["userScope", "listingId"],
    indices = [Index(value = ["userScope", "addedAt"])]
)
data class ComparisonSelectionEntity(
    val userScope: String,
    val listingId: String,
    val addedAt: Long
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val cacheKey: String,
    val lastSyncedAt: Long,
    val ttlMillis: Long,
    val nextCursor: String?,
    val lastError: String?
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeModeName: String = "LIGHT",
    val dataSaverEnabled: Boolean = true,
    val wifiOnlyPrefetch: Boolean = false,
    val defaultProvince: String? = null
)
