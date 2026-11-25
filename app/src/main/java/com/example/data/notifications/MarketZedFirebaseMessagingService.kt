package com.example.data.notifications

import com.example.MarketZedApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MarketZedFirebaseMessagingService : FirebaseMessagingService() {

    private fun notificationRepo(): NotificationPreferencesRepository {
        val app = applicationContext as? MarketZedApplication
        return app?.container?.notificationPreferencesRepository
            ?: NotificationPreferencesRepository(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val repo = notificationRepo()
        repo.saveFcmToken(token)
        repo.syncFcmTopics()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val title = remoteMessage.notification?.title?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: data["title"]?.trim().orEmpty()
        val body = remoteMessage.notification?.body?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: data["body"]?.trim()
            ?: data["message"]?.trim().orEmpty()

        if (title.isBlank() && body.isBlank()) return

        val kind = NotificationTargetParser.classifyKind(
            type = data["type"],
            channel = data["channel"] ?: data["topic"]
        )
        val dedupeKey = data["dedupeKey"]
            ?: data["notificationId"]
            ?: remoteMessage.messageId
            ?: "${kind.name}_${title}_${body}".hashCode().toString()

        val repo = notificationRepo()
        if (!repo.canDisplayIncomingMessage(kind, dedupeKey, MarketZedApplication.isForeground)) {
            return
        }

        val resolvedTitle = title.ifBlank { "MarketZed Update" }
        val resolvedBody = body.ifBlank { resolvedTitle }
        val listingId = data["listingId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["productId"]?.trim()?.takeIf { it.isNotEmpty() }
        val categoryId = data["categoryId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["category"]?.trim()?.takeIf { it.isNotEmpty() }

        if (kind == NotificationKind.ANNOUNCEMENT || kind == NotificationKind.PROMOTION) {
            repo.addAnnouncement(
                LocalAnnouncement(
                    id = dedupeKey,
                    title = resolvedTitle,
                    message = resolvedBody,
                    type = kind.name.lowercase(),
                    listingId = listingId,
                    categoryId = categoryId
                )
            )
        }

        val targetData = data.toMutableMap().apply {
            putIfAbsent("notificationId", dedupeKey)
            putIfAbsent("type", kind.name.lowercase())
            putIfAbsent("title", resolvedTitle)
            putIfAbsent("body", resolvedBody)
        }
        val target = NotificationTargetParser.parsePayload(targetData)

        if (MarketZedApplication.isForeground) {
            repo.emitForegroundNotice(
                ForegroundPushNotice(
                    id = dedupeKey,
                    title = resolvedTitle,
                    message = resolvedBody,
                    kind = kind,
                    target = target
                )
            )
        } else {
            MarketZedNotificationHelper.showNotification(
                context = applicationContext,
                title = resolvedTitle,
                body = resolvedBody,
                kind = kind,
                data = targetData,
                notificationId = dedupeKey.hashCode()
            )
        }
    }
}
