package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminOverview(
    val totalListings: Int = 0,
    val liveListings: Int = 0,
    val pendingReviewListings: Int = 0,
    val hiddenListings: Int = 0,
    val removedListings: Int = 0,
    val promotedListings: Int = 0,
    val activePromotions: Int = 0,
    val pendingPromotions: Int = 0,
    val openReports: Int = 0,
    val totalUsers: Int = 0,
    val verifiedUsers: Int = 0,
    val activatedUsers: Int = 0,
    val totalPayments: Int = 0,
    val totalRevenueZmw: Double = 0.0,
    val recentActions: List<AdminActivityLog> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AdminActivityLog(
    val id: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val summary: String = "",
    val actorEmail: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminPromotionRecord(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String? = null,
    val userId: String? = null,
    val userEmail: String? = null,
    val sellerName: String? = null,
    val durationDays: Int = 7,
    val status: String = "",
    val startsAt: String? = null,
    val expiresAt: String? = null,
    val amount: Double? = null,
    val currency: String = "ZMW",
    val createdAt: String? = null,
    val campaignFormat: String? = null,
    val impressions: Int? = null,
    val clicks: Int? = null,
    val visits: Int? = null
)

@JsonClass(generateAdapter = true)
data class AdminReportRecord(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String? = null,
    val reason: String = "",
    val details: String? = null,
    val reporterId: String? = null,
    val reporterEmail: String? = null,
    val status: String = "open",
    val resolutionNote: String? = null,
    val createdAt: String? = null,
    val resolvedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminUserRecord(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val contactNumber: String = "",
    val accountType: String = "individual",
    val province: String? = null,
    val town: String? = null,
    val role: String? = null,
    val verified: Boolean = false,
    val postingEnabled: Boolean = false,
    val verificationPaymentStatus: String? = null,
    val suspended: Boolean = false,
    val listingsCount: Int = 0,
    val createdAt: String? = null
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifEmpty { email.substringBefore("@").ifEmpty { uid.ifEmpty { "Member" } } }

    val isPostingActivated: Boolean
        get() = postingEnabled && verificationPaymentStatus.equals("paid", ignoreCase = true)

    val isAdminUser: Boolean
        get() = role?.trim()?.lowercase() in setOf("admin", "super_admin", "superadmin")
}

@JsonClass(generateAdapter = true)
data class AdminPaymentRecord(
    val id: String = "",
    val referenceId: String? = null,
    val userId: String? = null,
    val userEmail: String? = null,
    val payerName: String? = null,
    val purpose: String = "",
    val amount: Double? = null,
    val currency: String = "ZMW",
    val status: String = "pending",
    val listingId: String? = null,
    val durationDays: Int? = null,
    val createdAt: String? = null
)
