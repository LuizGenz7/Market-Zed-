package com.example.data.notifications

import android.content.Intent
import android.net.Uri

enum class NotificationKind {
    GENERAL,
    ANNOUNCEMENT,
    PROMOTION,
    ENGAGEMENT;

    val isPromotionalOrEngagement: Boolean
        get() = this == PROMOTION || this == ENGAGEMENT
}

data class NotificationPreferences(
    val generalEnabled: Boolean = true,
    val promotionalEnabled: Boolean = true,
    val smartEngagementEnabled: Boolean = true,
    val motivationalEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val fcmToken: String? = null
)

data class LocalAnnouncement(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val listingId: String? = null,
    val categoryId: String? = null,
    val receivedAtMillis: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

sealed class AppNavigationTarget {
    data class ListingDetail(val listingId: String) : AppNavigationTarget()
    data class Category(val categoryId: String) : AppNavigationTarget()
    data class Explore(val query: String? = null) : AppNavigationTarget()
    data object Notifications : AppNavigationTarget()
    data object Promotions : AppNavigationTarget()
    data object Wanted : AppNavigationTarget()
    data object Account : AppNavigationTarget()
    data object Home : AppNavigationTarget()
    data class Announcement(
        val id: String,
        val title: String,
        val message: String,
        val categoryId: String? = null,
        val listingId: String? = null
    ) : AppNavigationTarget()
}

object NotificationTargetParser {
    const val EXTRA_NOTIF_ID = "mz_notif_id"
    const val EXTRA_NOTIF_TYPE = "mz_notif_type"
    const val EXTRA_NOTIF_TITLE = "mz_notif_title"
    const val EXTRA_NOTIF_BODY = "mz_notif_body"
    const val EXTRA_LISTING_ID = "mz_listing_id"
    const val EXTRA_CATEGORY_ID = "mz_category_id"
    const val EXTRA_ROUTE = "mz_route"
    const val EXTRA_QUERY = "mz_query"

    fun parsePayload(data: Map<String, String>): AppNavigationTarget? {
        val listingId = data["listingId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["productId"]?.trim()?.takeIf { it.isNotEmpty() }
        val categoryId = data["categoryId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["category"]?.trim()?.takeIf { it.isNotEmpty() }
        val route = data["route"]?.trim()?.lowercase()
            ?: data["screen"]?.trim()?.lowercase()
        val type = data["type"]?.trim()?.lowercase().orEmpty()
        val query = data["query"]?.trim()?.takeIf { it.isNotEmpty() }
        val deepLink = data["deepLink"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["url"]?.trim()?.takeIf { it.isNotEmpty() }

        if (deepLink != null) {
            parseUri(Uri.parse(deepLink))?.let { return it }
        }

        if (listingId != null) {
            return AppNavigationTarget.ListingDetail(listingId)
        }
        if (categoryId != null) {
            return AppNavigationTarget.Category(categoryId)
        }

        val title = data["title"]?.trim().orEmpty()
        val body = data["body"]?.trim() ?: data["message"]?.trim().orEmpty()
        if ((type.contains("announcement") || type.contains("broadcast") || type.contains("notice") || route == "announcement") &&
            (title.isNotBlank() || body.isNotBlank())
        ) {
            return AppNavigationTarget.Announcement(
                id = data["notificationId"] ?: data["dedupeKey"] ?: "ann_${title.hashCode()}",
                title = title.ifBlank { "MarketZed Announcement" },
                message = body,
                categoryId = categoryId,
                listingId = listingId
            )
        }

        return when (route) {
            "explore" -> AppNavigationTarget.Explore(query)
            "wanted" -> AppNavigationTarget.Wanted
            "account", "profile" -> AppNavigationTarget.Account
            "notifications" -> AppNavigationTarget.Notifications
            "promotions", "sponsor" -> AppNavigationTarget.Promotions
            "home" -> AppNavigationTarget.Home
            else -> when {
                type.contains("promotion") -> AppNavigationTarget.Promotions
                type.contains("wanted") -> AppNavigationTarget.Wanted
                type.isNotBlank() -> AppNavigationTarget.Notifications
                else -> null
            }
        }
    }

    fun parseIntent(intent: Intent?): AppNavigationTarget? {
        if (intent == null) return null
        parseUri(intent.data)?.let { return it }

        val listingId = intent.getStringExtra(EXTRA_LISTING_ID)?.trim()?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra("listingId")?.trim()?.takeIf { it.isNotEmpty() }
        if (listingId != null) {
            return AppNavigationTarget.ListingDetail(listingId)
        }

        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)?.trim()?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra("categoryId")?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null) {
            return AppNavigationTarget.Category(categoryId)
        }

        val route = intent.getStringExtra(EXTRA_ROUTE)?.trim()?.lowercase()
            ?: intent.getStringExtra("route")?.trim()?.lowercase()
        val type = intent.getStringExtra(EXTRA_NOTIF_TYPE)?.trim()?.lowercase()
            ?: intent.getStringExtra("type")?.trim()?.lowercase().orEmpty()
        val title = intent.getStringExtra(EXTRA_NOTIF_TITLE)?.trim()
            ?: intent.getStringExtra("title")?.trim().orEmpty()
        val body = intent.getStringExtra(EXTRA_NOTIF_BODY)?.trim()
            ?: intent.getStringExtra("body")?.trim()
            ?: intent.getStringExtra("message")?.trim().orEmpty()
        val notifId = intent.getStringExtra(EXTRA_NOTIF_ID) ?: "ann_${title.hashCode()}"
        val query = intent.getStringExtra(EXTRA_QUERY)?.trim()?.takeIf { it.isNotEmpty() }

        if ((type.contains("announcement") || type.contains("broadcast") || type.contains("notice") || route == "announcement") &&
            (title.isNotBlank() || body.isNotBlank())
        ) {
            return AppNavigationTarget.Announcement(
                id = notifId,
                title = title.ifBlank { "MarketZed Announcement" },
                message = body
            )
        }

        return when (route) {
            "explore" -> AppNavigationTarget.Explore(query)
            "wanted" -> AppNavigationTarget.Wanted
            "account", "profile" -> AppNavigationTarget.Account
            "notifications" -> AppNavigationTarget.Notifications
            "promotions", "sponsor" -> AppNavigationTarget.Promotions
            "home" -> AppNavigationTarget.Home
            else -> null
        }
    }

    fun parseUri(uri: Uri?): AppNavigationTarget? {
        if (uri == null) return null
        val segments = uri.pathSegments.filter { it.isNotBlank() }
        return when {
            uri.scheme == "marketzed" && uri.host == "listings" && segments.isNotEmpty() ->
                AppNavigationTarget.ListingDetail(segments.first())
            uri.scheme == "marketzed" && uri.host == "category" && segments.isNotEmpty() ->
                AppNavigationTarget.Category(segments.first())
            uri.scheme == "marketzed" && uri.host == "explore" ->
                AppNavigationTarget.Explore(uri.getQueryParameter("q"))
            uri.scheme == "marketzed" && uri.host == "wanted" ->
                AppNavigationTarget.Wanted
            uri.scheme == "marketzed" && uri.host == "account" ->
                AppNavigationTarget.Account
            uri.scheme == "marketzed" && uri.host == "notifications" ->
                AppNavigationTarget.Notifications
            uri.scheme == "marketzed" && uri.host == "promotions" ->
                AppNavigationTarget.Promotions
            uri.scheme == "marketzed" && uri.host == "home" ->
                AppNavigationTarget.Home
            segments.size >= 2 && segments[0].equals("listings", ignoreCase = true) ->
                AppNavigationTarget.ListingDetail(segments[1])
            segments.size >= 2 && segments[0].equals("category", ignoreCase = true) ->
                AppNavigationTarget.Category(segments[1])
            else -> null
        }
    }

    fun classifyKind(type: String?, channel: String? = null): NotificationKind {
        val raw = "${type.orEmpty()} ${channel.orEmpty()}".lowercase()
        return when {
            raw.contains("engagement") || raw.contains("reminder") || raw.contains("inactive") ->
                NotificationKind.ENGAGEMENT
            raw.contains("promo") || raw.contains("featured") || raw.contains("deal") || raw.contains("new_arrival") ->
                NotificationKind.PROMOTION
            raw.contains("broadcast") || raw.contains("announcement") || raw.contains("admin") || raw.contains("notice") ->
                NotificationKind.ANNOUNCEMENT
            else -> NotificationKind.GENERAL
        }
    }
}
