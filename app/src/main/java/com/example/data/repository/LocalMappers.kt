package com.example.data.repository

import com.example.data.local.AppSettingsEntity
import com.example.data.local.CachedListingEntity
import com.example.data.local.ListingDraftEntity
import com.example.data.local.SavedSearchEntity
import com.example.data.model.AppSettings
import com.example.data.model.CategoryDetails
import com.example.data.model.ContactPreferences
import com.example.data.model.Listing
import com.example.data.model.ListingDraft
import com.example.data.model.ListingImage
import com.example.data.model.ListingLocation
import com.example.data.model.SavedSearch
import com.example.data.remote.NetworkClient
import com.squareup.moshi.Types

object LocalMappers {
    private val moshi = NetworkClient.moshi
    private val imageListType = Types.newParameterizedType(List::class.java, ListingImage::class.java)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringMapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)

    private val imageListAdapter = moshi.adapter<List<ListingImage>>(imageListType)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    private val stringMapAdapter = moshi.adapter<Map<String, String>>(stringMapType)
    private val contactAdapter = moshi.adapter(ContactPreferences::class.java)
    private val categoryDetailsAdapter = moshi.adapter(CategoryDetails::class.java)

    fun Listing.toCachedEntity(sectionKey: String, nowMillis: Long = System.currentTimeMillis()): CachedListingEntity {
        val mergedSpecs = linkedMapOf<String, String>().apply {
            putAll(specifications)
            location?.district?.trim()?.takeIf { it.isNotEmpty() }?.let { put(KEY_META_DISTRICT, it) }
            location?.nearbyArea?.trim()?.takeIf { it.isNotEmpty() }?.let { put(KEY_META_NEARBY_AREA, it) }
        }
        return CachedListingEntity(
            id = id,
            sectionKey = sectionKey,
            ownerId = ownerId,
            sellerName = sellerName,
            sellerVerified = sellerVerified,
            isPromoted = isPromoted,
            categoryId = categoryId,
            subcategory = subcategory,
            title = title,
            description = description,
            price = price,
            currency = currency,
            priceMode = priceMode,
            province = location?.province.orEmpty(),
            town = location?.town.orEmpty(),
            imagesJson = runCatching { imageListAdapter.toJson(images) }.getOrDefault("[]"),
            contactPreferencesJson = contactPreferences?.let { runCatching { contactAdapter.toJson(it) }.getOrNull() },
            categoryDetailsJson = categoryDetails?.let { runCatching { categoryDetailsAdapter.toJson(it) }.getOrNull() },
            moderationStatus = moderationStatus,
            availabilityStatus = availabilityStatus,
            wantedStatus = wantedStatus,
            isPublic = isPublic != false,
            reviewRequired = reviewRequired,
            createdAt = createdAt,
            updatedAt = updatedAt,
            publishedAt = publishedAt,
            expiresAt = expiresAt,
            cachedAt = nowMillis,
            originalPrice = originalPrice,
            sellerPhotoUrl = sellerPhotoUrl,
            sellerAccountType = sellerAccountType,
            sellerBio = sellerBio,
            sellerMemberSince = sellerMemberSince,
            specificationsJson = if (mergedSpecs.isEmpty()) null else runCatching { stringMapAdapter.toJson(mergedSpecs) }.getOrNull(),
            campaignFormat = campaignFormat,
            impressionsCount = impressionsCount,
            clicksCount = clicksCount,
            visitsCount = visitsCount
        )
    }

    fun CachedListingEntity.toDomain(): Listing {
        val parsedImages = runCatching { imageListAdapter.fromJson(imagesJson) }.getOrNull().orEmpty()
        val parsedContact = contactPreferencesJson?.let {
            runCatching { contactAdapter.fromJson(it) }.getOrNull()
        }
        val parsedDetails = categoryDetailsJson?.let {
            runCatching { categoryDetailsAdapter.fromJson(it) }.getOrNull()
        }
        val rawSpecs = specificationsJson?.let {
            runCatching { stringMapAdapter.fromJson(it) }.getOrNull()
        }.orEmpty()
        val decodedDistrict = rawSpecs[KEY_META_DISTRICT].orEmpty()
        val decodedNearbyArea = rawSpecs[KEY_META_NEARBY_AREA]
        val parsedSpecs = rawSpecs.filterKeys { !it.startsWith("__") }
        return Listing(
            id = id,
            ownerId = ownerId,
            sellerName = sellerName,
            sellerVerified = sellerVerified,
            isPromoted = isPromoted,
            categoryId = categoryId,
            subcategory = subcategory,
            title = title,
            description = description,
            price = price,
            currency = currency,
            priceMode = priceMode,
            location = ListingLocation(
                province = province,
                town = town,
                district = decodedDistrict,
                nearbyArea = decodedNearbyArea
            ),
            images = parsedImages,
            contactPreferences = parsedContact,
            categoryDetails = parsedDetails,
            moderationStatus = moderationStatus,
            availabilityStatus = availabilityStatus,
            wantedStatus = wantedStatus,
            isPublic = isPublic,
            reviewRequired = reviewRequired,
            createdAt = createdAt,
            updatedAt = updatedAt,
            publishedAt = publishedAt,
            expiresAt = expiresAt,
            originalPrice = originalPrice,
            sellerPhotoUrl = sellerPhotoUrl,
            sellerAccountType = sellerAccountType,
            sellerBio = sellerBio,
            sellerMemberSince = sellerMemberSince,
            specifications = parsedSpecs,
            campaignFormat = campaignFormat,
            impressionsCount = impressionsCount,
            clicksCount = clicksCount,
            visitsCount = visitsCount
        )
    }

    private const val KEY_META_DISTRICT = "__district"
    private const val KEY_META_NEARBY_AREA = "__nearbyArea"

    fun SavedSearchEntity.toDomain(): SavedSearch = SavedSearch(
        id = id,
        userScope = userScope,
        name = name,
        query = query,
        category = category,
        subcategory = subcategory,
        province = province,
        town = town,
        minPrice = minPrice,
        maxPrice = maxPrice,
        condition = condition,
        sort = sort,
        notifyMatches = notifyMatches,
        createdAt = createdAt,
        lastMatchedAt = lastMatchedAt
    )

    fun SavedSearch.toEntity(): SavedSearchEntity = SavedSearchEntity(
        id = id,
        userScope = userScope,
        name = name,
        query = query,
        category = category,
        subcategory = subcategory,
        province = province,
        town = town,
        minPrice = minPrice,
        maxPrice = maxPrice,
        condition = condition,
        sort = sort,
        notifyMatches = notifyMatches,
        createdAt = createdAt,
        lastMatchedAt = lastMatchedAt
    )

    private const val KEY_DRAFT_DISTRICT = "__draftDistrict"
    private const val KEY_DRAFT_NEARBY_AREA = "__draftNearbyArea"

    fun ListingDraft.toEntity(): ListingDraftEntity {
        val mergedSpecs = linkedMapOf<String, String>().apply {
            categoryDetails.specs?.forEach { (k, v) -> if (!k.startsWith("__")) put(k, v) }
            district.trim().takeIf { it.isNotEmpty() }?.let { put(KEY_DRAFT_DISTRICT, it) }
            nearbyArea.trim().takeIf { it.isNotEmpty() }?.let { put(KEY_DRAFT_NEARBY_AREA, it) }
        }
        val persistedDetails = categoryDetails.copy(
            specs = mergedSpecs.takeIf { it.isNotEmpty() }
        )
        return ListingDraftEntity(
            localUuid = localUuid,
            userScope = userScope,
            step = step,
            categoryId = categoryId,
            subcategory = subcategory,
            title = title,
            description = description,
            price = price,
            province = province,
            town = town,
            categoryDetailsJson = runCatching { categoryDetailsAdapter.toJson(persistedDetails) }.getOrDefault("{}"),
            contactPreferencesJson = runCatching { contactAdapter.toJson(contactPreferences) }.getOrDefault("{}"),
            localImagePathsJson = runCatching { stringListAdapter.toJson(localImagePaths) }.getOrDefault("[]"),
            uploadedImagesJson = runCatching { imageListAdapter.toJson(uploadedImages) }.getOrDefault("[]"),
            ownershipDeclarationAccepted = ownershipDeclarationAccepted,
            ownershipDeclarationVersion = ownershipDeclarationVersion,
            updatedAt = updatedAt
        )
    }

    fun ListingDraftEntity.toDomain(): ListingDraft {
        val rawDetails = runCatching { categoryDetailsAdapter.fromJson(categoryDetailsJson) }.getOrNull()
            ?: CategoryDetails()
        val rawSpecs = rawDetails.specs.orEmpty()
        val decodedDistrict = rawSpecs[KEY_DRAFT_DISTRICT].orEmpty()
        val decodedNearbyArea = rawSpecs[KEY_DRAFT_NEARBY_AREA].orEmpty()
        val cleanedSpecs = rawSpecs.filterKeys { !it.startsWith("__") }.takeIf { it.isNotEmpty() }
        return ListingDraft(
            localUuid = localUuid,
            userScope = userScope,
            step = step,
            categoryId = categoryId,
            subcategory = subcategory,
            title = title,
            description = description,
            price = price,
            province = province,
            district = decodedDistrict,
            town = town,
            nearbyArea = decodedNearbyArea,
            categoryDetails = rawDetails.copy(specs = cleanedSpecs),
            contactPreferences = runCatching { contactAdapter.fromJson(contactPreferencesJson) }.getOrNull()
                ?: ContactPreferences(),
            localImagePaths = runCatching { stringListAdapter.fromJson(localImagePathsJson) }.getOrNull().orEmpty(),
            uploadedImages = runCatching { imageListAdapter.fromJson(uploadedImagesJson) }.getOrNull().orEmpty(),
            ownershipDeclarationAccepted = ownershipDeclarationAccepted,
            ownershipDeclarationVersion = ownershipDeclarationVersion,
            updatedAt = updatedAt
        )
    }

    fun AppSettingsEntity?.toDomain(): AppSettings {
        if (this == null) return AppSettings()
        return AppSettings(
            themeModeName = themeModeName,
            dataSaverEnabled = dataSaverEnabled,
            wifiOnlyPrefetch = wifiOnlyPrefetch,
            defaultProvince = defaultProvince
        )
    }

    fun AppSettings.toEntity(): AppSettingsEntity {
        return AppSettingsEntity(
            id = 1,
            themeModeName = themeModeName,
            dataSaverEnabled = dataSaverEnabled,
            wifiOnlyPrefetch = wifiOnlyPrefetch,
            defaultProvince = defaultProvince
        )
    }
}
