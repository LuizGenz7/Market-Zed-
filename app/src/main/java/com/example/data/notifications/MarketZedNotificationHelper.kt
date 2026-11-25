package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object MarketZedNotificationHelper {

    const val CHANNEL_GENERAL = "marketzed_general"
    const val CHANNEL_PROMOTIONS = "marketzed_promotions"
    const val CHANNEL_ENGAGEMENT = "marketzed_engagement"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            "General & Announcements",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important MarketZed notices, account alerts, and platform announcements."
        }

        val promoChannel = NotificationChannel(
            CHANNEL_PROMOTIONS,
            "Promotions & Sponsored",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Sponsored deals, new arrivals, and promotional updates."
        }

        val engagementChannel = NotificationChannel(
            CHANNEL_ENGAGEMENT,
            "Marketplace Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Occasional highlights of relevant listings and opportunities."
        }

        manager.createNotificationChannels(
            listOf(generalChannel, promoChannel, engagementChannel)
        )
    }

    fun canPostSystemNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(false)
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        kind: NotificationKind,
        data: Map<String, String> = emptyMap(),
        notificationId: Int = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
    ): Boolean {
        ensureChannelsCreated(context)
        if (!canPostSystemNotifications(context)) return false

        val channelId = when (kind) {
            NotificationKind.GENERAL, NotificationKind.ANNOUNCEMENT -> CHANNEL_GENERAL
            NotificationKind.PROMOTION -> CHANNEL_PROMOTIONS
            NotificationKind.ENGAGEMENT -> CHANNEL_ENGAGEMENT
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.techgu.marketzed.OPEN_NOTIFICATION_$notificationId"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationTargetParser.EXTRA_NOTIF_ID, data["notificationId"] ?: data["dedupeKey"] ?: notificationId.toString())
            putExtra(NotificationTargetParser.EXTRA_NOTIF_TYPE, data["type"] ?: kind.name.lowercase())
            putExtra(NotificationTargetParser.EXTRA_NOTIF_TITLE, title)
            putExtra(NotificationTargetParser.EXTRA_NOTIF_BODY, body)
            (data["listingId"] ?: data["productId"])?.let {
                putExtra(NotificationTargetParser.EXTRA_LISTING_ID, it)
            }
            (data["categoryId"] ?: data["category"])?.let {
                putExtra(NotificationTargetParser.EXTRA_CATEGORY_ID, it)
            }
            (data["route"] ?: data["screen"])?.let {
                putExtra(NotificationTargetParser.EXTRA_ROUTE, it)
            }
            data["query"]?.let {
                putExtra(NotificationTargetParser.EXTRA_QUERY, it)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ContextCompat.getColor(context, R.color.marketzed_orange))
            .setContentTitle(title.ifBlank { "MarketZed" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (kind == NotificationKind.GENERAL || kind == NotificationKind.ANNOUNCEMENT) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        }.getOrDefault(false)
    }
}
