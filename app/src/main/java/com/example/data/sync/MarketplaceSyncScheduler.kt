package com.example.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.TimeUnit

data class BackgroundSyncStatus(
    val statusLabel: String = "Scheduled",
    val isRunning: Boolean = false,
    val wifiOnlyConstraint: Boolean = false,
    val dataSaverEnabled: Boolean = true,
    val syncIntervalHours: Long = 6L,
    val lastSyncedAt: Long? = null,
    val lastSummary: String? = null,
    val lastError: String? = null,
    val runAttemptCount: Int = 0
)

class MarketplaceSyncScheduler(
    private val context: Context
) {
    companion object {
        const val UNIQUE_PERIODIC_SYNC_NAME = "marketzed_periodic_sync"
        const val UNIQUE_IMMEDIATE_SYNC_NAME = "marketzed_immediate_sync"
        const val UNIQUE_ENGAGEMENT_WORK_NAME = "marketzed_smart_engagement"
        const val UNIQUE_MOTIVATIONAL_WORK_NAME = "marketzed_motivational_notifications"
        const val TAG_MARKETZED_SYNC = "marketzed_sync"

        const val INTERVAL_DATA_SAVER_HOURS = 6L
        const val FLEX_DATA_SAVER_MINUTES = 60L
        const val INTERVAL_STANDARD_HOURS = 2L
        const val FLEX_STANDARD_MINUTES = 30L
    }

    private fun workManagerOrNull(): WorkManager? {
        return runCatching { WorkManager.getInstance(context.applicationContext) }.getOrNull()
    }

    fun scheduleSmartEngagementWorker(enabled: Boolean) {
        val wm = workManagerOrNull() ?: return
        if (!enabled) {
            wm.cancelUniqueWork(UNIQUE_ENGAGEMENT_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SmartEngagementWorker>(
            24L,
            TimeUnit.HOURS,
            4L,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(UNIQUE_ENGAGEMENT_WORK_NAME)
            .build()

        wm.enqueueUniquePeriodicWork(
            UNIQUE_ENGAGEMENT_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleMotivationalNotifications(enabled: Boolean) {
        val wm = workManagerOrNull() ?: return
        if (!enabled) {
            wm.cancelUniqueWork(UNIQUE_MOTIVATIONAL_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<MotivationalNotificationWorker>(
            24L,
            TimeUnit.HOURS,
            4L,
            TimeUnit.HOURS
        )
            .setInitialDelay(6L, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(UNIQUE_MOTIVATIONAL_WORK_NAME)
            .build()

        wm.enqueueUniquePeriodicWork(
            UNIQUE_MOTIVATIONAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun schedulePeriodicSync(
        wifiOnly: Boolean,
        dataSaverEnabled: Boolean
    ) {
        val wm = workManagerOrNull() ?: return
        val intervalHours = if (dataSaverEnabled) INTERVAL_DATA_SAVER_HOURS else INTERVAL_STANDARD_HOURS
        val flexMinutes = if (dataSaverEnabled) FLEX_DATA_SAVER_MINUTES else FLEX_STANDARD_MINUTES

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<MarketplaceSyncWorker>(
            intervalHours,
            TimeUnit.HOURS,
            flexMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .addTag(TAG_MARKETZED_SYNC)
            .setInputData(
                workDataOf(
                    MarketplaceSyncWorker.KEY_FORCE_REFRESH to false,
                    MarketplaceSyncWorker.KEY_RESPECT_WIFI_PREF to true,
                    MarketplaceSyncWorker.KEY_SYNC_SOURCE to "periodic"
                )
            )
            .build()

        wm.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun enqueueImmediateSync(
        force: Boolean = true,
        respectWifiPref: Boolean = false,
        source: String = "manual"
    ) {
        val wm = workManagerOrNull() ?: return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (respectWifiPref) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MarketplaceSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15L, TimeUnit.SECONDS)
            .addTag(TAG_MARKETZED_SYNC)
            .setInputData(
                workDataOf(
                    MarketplaceSyncWorker.KEY_FORCE_REFRESH to force,
                    MarketplaceSyncWorker.KEY_RESPECT_WIFI_PREF to respectWifiPref,
                    MarketplaceSyncWorker.KEY_SYNC_SOURCE to source
                )
            )
            .build()

        wm.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_SYNC_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAllSync() {
        workManagerOrNull()?.let { wm ->
            wm.cancelUniqueWork(UNIQUE_PERIODIC_SYNC_NAME)
            wm.cancelUniqueWork(UNIQUE_IMMEDIATE_SYNC_NAME)
        }
    }

    fun observeSyncStatus(
        marketplaceRepository: MarketplaceRepository,
        localUserRepository: LocalUserRepository
    ): Flow<BackgroundSyncStatus> {
        val wm = workManagerOrNull()
        val periodicFlow: Flow<List<WorkInfo>> = wm
            ?.getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_SYNC_NAME)
            ?.catch { emit(emptyList()) }
            ?: flowOf(emptyList())
        val immediateFlow: Flow<List<WorkInfo>> = wm
            ?.getWorkInfosForUniqueWorkFlow(UNIQUE_IMMEDIATE_SYNC_NAME)
            ?.catch { emit(emptyList()) }
            ?: flowOf(emptyList())

        return combine(
            periodicFlow,
            immediateFlow,
            marketplaceRepository.observeBackgroundSyncMetadata(),
            localUserRepository.observeSettings()
        ) { periodicInfos, immediateInfos, meta, settings ->
            val immediateInfo = immediateInfos.firstOrNull()
            val periodicInfo = periodicInfos.firstOrNull()
            val activeInfo = immediateInfo?.takeIf {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            } ?: periodicInfo ?: immediateInfo

            val isRunning = immediateInfo?.state == WorkInfo.State.RUNNING ||
                periodicInfo?.state == WorkInfo.State.RUNNING

            val statusLabel = when {
                isRunning -> "Syncing..."
                immediateInfo?.state == WorkInfo.State.ENQUEUED -> "Queued (Immediate)"
                immediateInfo?.state == WorkInfo.State.FAILED -> "Last sync failed"
                meta?.nextCursor?.startsWith("Skipped:") == true -> "Waiting for Wi-Fi"
                meta?.lastError != null -> "Sync warning"
                periodicInfo?.state == WorkInfo.State.ENQUEUED -> "Scheduled"
                meta?.lastSyncedAt != null && meta.lastSyncedAt > 0L -> "Up to date"
                else -> "Scheduled"
            }

            BackgroundSyncStatus(
                statusLabel = statusLabel,
                isRunning = isRunning,
                wifiOnlyConstraint = settings.wifiOnlyPrefetch,
                dataSaverEnabled = settings.dataSaverEnabled,
                syncIntervalHours = if (settings.dataSaverEnabled) INTERVAL_DATA_SAVER_HOURS else INTERVAL_STANDARD_HOURS,
                lastSyncedAt = meta?.lastSyncedAt?.takeIf { it > 0L },
                lastSummary = meta?.nextCursor,
                lastError = meta?.lastError,
                runAttemptCount = activeInfo?.runAttemptCount ?: 0
            )
        }
    }
}
