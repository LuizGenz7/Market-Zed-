package com.example.data.model

import com.example.core.util.DateUtils
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CategoryOption(
    val id: String,
    val name: String,
    val description: String,
    val subcategories: List<String>,
    val requiresImage: Boolean
)

@JsonClass(generateAdapter = true)
data class ProvinceLocations(
    val province: String,
    val towns: List<String>
)

@JsonClass(generateAdapter = true)
data class ListingImage(
    val url: String = "",
    val thumbUrl: String? = null,
    val publicId: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val format: String? = null
)

@JsonClass(generateAdapter = true)
data class ListingLocation(
    val province: String = "",
    val town: String = "",
    val district: String = "",
    val nearbyArea: String? = null
) {
    val displayTown: String
        get() = town.trim()
            .ifEmpty { district.trim() }
            .ifEmpty { province.trim() }
            .ifEmpty { "Location not specified" }

    val displayFull: String
        get() {
            val parts = buildList {
                val cleanArea = nearbyArea?.trim().orEmpty()
                val cleanTown = town.trim()
                val cleanDistrict = district.trim()
                val cleanProvince = province.trim()
                if (cleanArea.isNotEmpty() && !cleanArea.equals(cleanTown, ignoreCase = true)) {
                    add(cleanArea)
                }
                if (cleanTown.isNotEmpty()) {
                    add(cleanTown)
                }
                if (cleanDistrict.isNotEmpty() &&
                    !cleanDistrict.equals(cleanTown, ignoreCase = true) &&
                    !cleanDistrict.equals(cleanProvince, ignoreCase = true)
                ) {
                    add("$cleanDistrict District")
                }
                if (cleanProvince.isNotEmpty() && !cleanProvince.equals(cleanTown, ignoreCase = true)) {
                    add(cleanProvince)
                }
            }
            return if (parts.isEmpty()) "Location not specified" else parts.joinToString(", ")
        }
}

@JsonClass(generateAdapter = true)
data class ContactPreferences(
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null
) {
    val hasAny: Boolean
        get() = !phone.isNullOrBlank() || !whatsapp.isNullOrBlank() || !email.isNullOrBlank()
}

@JsonClass(generateAdapter = true)
data class CategoryDetails(
    val condition: String? = null,
    val bedrooms: String? = null,
    val bathrooms: String? = null,
    val amenities: String? = null,
    val employerName: String? = null,
    val employmentType: String? = null,
    val swapFor: String? = null,
    val serviceArea: String? = null,
    val wantedDetails: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val specs: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class Listing(
    val id: String,
    val ownerId: String? = null,
    val sellerName: String? = null,
    val sellerVerified: Boolean = false,
    val isPromoted: Boolean = false,
    val categoryId: String = "marketplace",
    val subcategory: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double? = null,
    val currency: String = "ZMW",
    val priceMode: String? = null,
    val location: ListingLocation? = null,
    val images: List<ListingImage> = emptyList(),
    val contactPreferences: ContactPreferences? = null,
    val categoryDetails: CategoryDetails? = null,
    val moderationStatus: String? = null,
    val availabilityStatus: String? = null,
    val wantedStatus: String? = null,
    val isPublic: Boolean? = true,
    val reviewRequired: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publishedAt: String? = null,
    val expiresAt: String? = null,
    val originalPrice: Double? = null,
    val sellerPhotoUrl: String? = null,
    val sellerAccountType: String? = null,
    val sellerBio: String? = null,
    val sellerMemberSince: String? = null,
    val specifications: Map<String, String> = emptyMap(),
    val campaignFormat: String? = null,
    val impressionsCount: Int? = null,
    val clicksCount: Int? = null,
    val visitsCount: Int? = null
) {
    val isWanted: Boolean
        get() = categoryId.equals("wanted", ignoreCase = true)

    val isComparableCategory: Boolean
        get() = categoryId.trim().lowercase() in setOf("marketplace", "property", "swaps", "free")

    val hasVerifiedPriceDrop: Boolean
        get() = originalPrice != null && price != null && price > 0.0 && originalPrice > price

    val priceDropPercent: Int?
        get() = if (hasVerifiedPriceDrop && originalPrice != null && price != null && originalPrice > 0.0) {
            (((originalPrice - price) / originalPrice) * 100.0).toInt().coerceIn(1, 99)
        } else {
            null
        }

    val resolvedSpecifications: Map<String, String>
        get() {
            val merged = linkedMapOf<String, String>()
            categoryDetails?.specs?.forEach { (k, v) ->
                if (k.isNotBlank() && v.isNotBlank()) merged[k.trim()] = v.trim()
            }
            specifications.forEach { (k, v) ->
                if (k.isNotBlank() && v.isNotBlank()) merged[k.trim()] = v.trim()
            }
            categoryDetails?.brand?.takeIf { it.isNotBlank() }?.let { merged.putIfAbsent("Brand", it.trim()) }
            categoryDetails?.model?.takeIf { it.isNotBlank() }?.let { merged.putIfAbsent("Model", it.trim()) }
            categoryDetails?.condition?.takeIf { it.isNotBlank() }?.let { merged.putIfAbsent("Condition", it.trim()) }
            return merged
        }

    val isEditableByOwner: Boolean
        get() = moderationStatus in setOf("live_needs_review", "live_reviewed", "published")

    val isHiddenOrRemoved: Boolean
        get() = moderationStatus in setOf("hidden", "removed", "rejected")

    val isEligiblePublic: Boolean
        get() {
            val mod = moderationStatus?.trim()?.lowercase()
            val allowedModeration = mod.isNullOrEmpty() ||
                mod in setOf("live_needs_review", "live_reviewed", "published", "approved", "active")
            val avail = availabilityStatus?.trim()?.lowercase()
            val available = avail.isNullOrEmpty() || avail == "available" || avail == "active"
            val publicOk = isPublic != false
            val wantedOk = if (isWanted) {
                !wantedStatus.equals("fulfilled", ignoreCase = true) && !DateUtils.isExpired(expiresAt)
            } else {
                true
            }
            val policyOk = !com.example.data.catalogue.MarketZedCatalogue.isProhibitedLandOrPropertySale(
                categoryId = categoryId,
                subcategory = subcategory,
                title = title,
                description = description,
                priceMode = priceMode
            )
            return allowedModeration && available && publicOk && wantedOk && policyOk
        }

    val createdAtMillis: Long
        get() = DateUtils.parseIsoToMillis(publishedAt)
            ?: DateUtils.parseIsoToMillis(createdAt)
            ?: 0L
}

data class SearchFilter(
    val query: String = "",
    val category: String? = null,
    val subcategory: String? = null,
    val province: String? = null,
    val district: String? = null,
    val town: String? = null,
    val nearbyArea: String? = null,
    val condition: String? = null,
    val sort: String = "newest",
    val minPrice: Double? = null,
    val maxPrice: Double? = null
) {
    fun cacheKey(): String {
        return listOf(
            query.trim().lowercase(),
            category.orEmpty(),
            subcategory.orEmpty(),
            province.orEmpty(),
            district.orEmpty(),
            town.orEmpty(),
            nearbyArea.orEmpty().trim().lowercase(),
            condition.orEmpty(),
            sort,
            minPrice?.toString().orEmpty(),
            maxPrice?.toString().orEmpty()
        ).joinToString("|")
    }
}

data class SavedSearch(
    val id: String,
    val userScope: String,
    val name: String,
    val query: String = "",
    val category: String? = null,
    val subcategory: String? = null,
    val province: String? = null,
    val town: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val condition: String? = null,
    val sort: String = "newest",
    val notifyMatches: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMatchedAt: Long? = null
) {
    fun toSearchFilter(): SearchFilter = SearchFilter(
        query = query,
        category = category,
        subcategory = subcategory,
        province = province,
        town = town,
        condition = condition,
        sort = sort,
        minPrice = minPrice,
        maxPrice = maxPrice
    )
}

enum class SponsoredFormat(val wireValue: String, val label: String, val placementSummary: String) {
    CAROUSEL_1_3_1("carousel_1_3_1", "Featured 1:3:1 Carousel", "Hero horizontal spotlight at the top of Home"),
    HOMEPAGE_BANNER("homepage_banner", "Homepage Banner", "Full-width promotional showcase on Home"),
    CATEGORY_BANNER("category_banner", "Category Top Banner", "Top placement inside relevant category feeds"),
    COMPACT_STRIP("compact_strip", "Compact Spotlight Strip", "High-visibility horizontal strip in Explore & Home"),
    SPONSORED_DEAL("sponsored_deal", "Sponsored Deals Spotlight", "Featured in Sponsored Deals & discovery")
}

data class SponsoredPlacementBanner(
    val listing: Listing,
    val format: SponsoredFormat,
    val headline: String,
    val subheadline: String
)

/**
 * Extensible promotional incentive model.
 * Any bonus duration, credit, or discount remains strictly gated behind [approvedByPolicy]
 * so unapproved entitlements are never fabricated client-side.
 */
data class PromotionalBonusRule(
    val id: String,
    val title: String,
    val description: String,
    val eligibleAccountType: String? = null,
    val approvedByPolicy: Boolean = false
)

data class SellerPublicProfile(
    val sellerKey: String,
    val ownerId: String?,
    val displayName: String,
    val accountType: String,
    val verified: Boolean,
    val photoUrl: String?,
    val bio: String?,
    val memberSince: String?,
    val primaryLocation: String?,
    val contactPreferences: ContactPreferences?,
    val listings: List<Listing>
)

data class CachedFeedResult<T>(
    val items: List<T>,
    val isStale: Boolean,
    val isOffline: Boolean,
    val lastSyncedAt: Long?,
    val nextCursor: String? = null,
    val errorMessage: String? = null
)

enum class AccountVerificationUiStatus {
    NOT_VERIFIED,
    PENDING,
    VERIFIED,
    FAILED
}

@JsonClass(generateAdapter = true)
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val contactNumber: String = "",
    val dateOfBirth: String? = null,
    val whatsappNumber: String? = null,
    val alternativeContact: String? = null,
    val accountType: String = "individual",
    val province: String? = null,
    val town: String? = null,
    val verificationPaymentStatus: String? = null,
    val postingEnabled: Boolean = false,
    val verified: Boolean = false,
    val freeWantedPostUsed: Boolean = false,
    val wantedPostsCount: Int = 0,
    val verificationStatus: String? = null,
    val verificationFeeZmw: Double? = null,
    val verificationPaymentId: String? = null,
    val verificationDate: String? = null,
    val role: String? = null,
    val isAdmin: Boolean = false,
    val admin: Boolean = false,
    val photoUrl: String? = null,
    val bio: String? = null,
    val memberSince: String? = null
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifEmpty { "MarketZed User" }

    val isPostingActivated: Boolean
        get() = postingEnabled && verificationPaymentStatus.equals("paid", ignoreCase = true)

    val hasVerifiedEntitlement: Boolean
        get() = verified ||
            isPostingActivated ||
            verificationStatus?.trim()?.lowercase() in setOf("verified", "approved", "active")

    val resolvedVerificationStatus: AccountVerificationUiStatus
        get() = when {
            hasVerifiedEntitlement -> AccountVerificationUiStatus.VERIFIED
            verificationStatus?.trim()?.lowercase() in setOf("pending", "processing", "submitted") ||
                verificationPaymentStatus?.trim()?.lowercase() in setOf("pending", "processing") ->
                AccountVerificationUiStatus.PENDING
            verificationStatus?.trim()?.lowercase() in setOf("failed", "rejected", "cancelled") ||
                verificationPaymentStatus?.trim()?.lowercase() in setOf("failed", "cancelled") ->
                AccountVerificationUiStatus.FAILED
            else -> AccountVerificationUiStatus.NOT_VERIFIED
        }

    val hasFreeWantedPostRemaining: Boolean
        get() = !freeWantedPostUsed && wantedPostsCount <= 0

    val hasAdminRole: Boolean
        get() = isAdmin || admin || role?.trim()?.lowercase() in setOf("admin", "super_admin", "superadmin")
}

@JsonClass(generateAdapter = true)
data class PaymentRecord(
    val id: String = "",
    val referenceId: String? = null,
    val purpose: String = "",
    val amount: Double? = null,
    val currency: String = "ZMW",
    val status: String = "pending",
    val listingId: String? = null,
    val durationDays: Int? = null,
    val createdAt: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class PromotionPricing(
    val prices: Map<String, Double> = emptyMap()
) {
    fun verificationFeeOrDefault(defaultFee: Double = 30.0): Double {
        return prices["account_verification"]
            ?: prices["verification"]
            ?: prices["k30_verification"]
            ?: defaultFee
    }
}

@JsonClass(generateAdapter = true)
data class PromotionRecord(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String? = null,
    val durationDays: Int = 7,
    val status: String = "",
    val startsAt: String? = null,
    val expiresAt: String? = null,
    val amount: Double? = null,
    val currency: String = "ZMW",
    val campaignFormat: String? = null,
    val impressions: Int? = null,
    val clicks: Int? = null,
    val listingVisits: Int? = null
)

@JsonClass(generateAdapter = true)
data class AppNotification(
    val id: String = "",
    val type: String = "",
    val title: String? = null,
    val message: String = "",
    val read: Boolean = false,
    val listingId: String? = null,
    val paymentId: String? = null,
    val categoryId: String? = null,
    val actionLabel: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Testimonial(
    val id: String = "",
    val name: String = "",
    val town: String = "Zambia",
    val message: String = "",
    val rating: Int = 5,
    val status: String = "approved",
    val createdAt: String? = null
)

data class ListingDraft(
    val localUuid: String,
    val userScope: String,
    val step: Int = 0,
    val categoryId: String = "marketplace",
    val subcategory: String = "",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val province: String = "Lusaka",
    val district: String = "",
    val town: String = "Lusaka",
    val nearbyArea: String = "",
    val categoryDetails: CategoryDetails = CategoryDetails(),
    val contactPreferences: ContactPreferences = ContactPreferences(),
    val localImagePaths: List<String> = emptyList(),
    val uploadedImages: List<ListingImage> = emptyList(),
    val ownershipDeclarationAccepted: Boolean = false,
    val ownershipDeclarationVersion: String = "2026-11-16-v1",
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val themeModeName: String = "LIGHT",
    val dataSaverEnabled: Boolean = true,
    val wifiOnlyPrefetch: Boolean = false,
    val defaultProvince: String? = null
)
