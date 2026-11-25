package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MarketZedApplication
import com.example.core.util.MotivationalMessageSelector
import com.example.data.notifications.MarketZedNotificationHelper
import com.example.data.notifications.NotificationKind
import com.example.data.notifications.NotificationPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scheduled WorkManager worker that delivers rotating, random encouraging messages
 * from [MotivationalMessageSelector] even when the app is closed and offline.
 */
class MotivationalNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val motivationalMessageSelector: MotivationalMessageSelector
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (MarketZedApplication.isForeground) {
            return@withContext Result.success()
        }
        if (!notificationPreferencesRepository.canSendMotivationalNotification()) {
            return@withContext Result.success()
        }
        if (!MarketZedNotificationHelper.canPostSystemNotifications(applicationContext)) {
            return@withContext Result.success()
        }

        val picked = motivationalMessageSelector.nextMessage()
        val payload = buildMap {
            put("notificationId", "motivational_${picked.id}")
            put("type", "engagement")
            put("title", picked.title)
            put("body", picked.subtitle)
            if (!picked.targetCategoryId.isNullOrBlank()) {
                put("categoryId", picked.targetCategoryId)
            } else {
                put("route", "explore")
            }
        }

        val posted = MarketZedNotificationHelper.showNotification(
            context = applicationContext,
            title = picked.title,
            body = picked.subtitle,
            kind = NotificationKind.ENGAGEMENT,
            data = payload,
            notificationId = ("motivational_${picked.id}").hashCode()
        )
        if (posted) {
            notificationPreferencesRepository.recordMotivationalSent()
        }

        Result.success()
    }
}
