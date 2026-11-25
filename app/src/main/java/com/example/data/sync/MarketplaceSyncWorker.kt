package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.MarketZedDao
import com.example.data.local.SyncMetadataEntity
import com.example.data.notifications.MarketZedNotificationHelper
import com.example.data.notifications.NotificationKind
import com.example.data.notifications.NotificationPreferencesRepository
import com.example.data.repository.HttpSyncException
import com.example.data.repository.LocalMappers.toDomain
import com.example.data.repository.MarketplaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * WorkManager CoroutineWorker that synchronizes safe public read feeds (`api/search`,
 * `api/promotions/pricing`) into Room SQLite and prunes stale search cache rows while
 * preserving favorites, recently viewed items, and local listing drafts.
 *
 * Strictly read-only sync: authenticated or sensitive mutations (listing submit, payments,
 * profile updates, reports) are never queued in the background.
 */
class MarketplaceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val marketplaceRepository: MarketplaceRepository,
    private val dao: MarketZedDao,
    private val notificationPreferencesRepository: NotificationPreferencesRepository =
        NotificationPreferencesRepository(appContext),
    private val unmeteredNetworkChecker: (Context) -> Boolean = ::hasUnmeteredNetwork
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)
        val respectWifiPref = inputData.getBoolean(KEY_RESPECT_WIFI_PREF, true)

        val settings = runCatching { dao.getSettings() }.getOrNull()
        if (respectWifiPref && settings?.wifiOnlyPrefetch == true && !unmeteredNetworkChecker(applicationContext)) {
            val prev = runCatching { dao.getSyncMetadata(MarketplaceRepository.META_WORK_SYNC) }.getOrNull()
            runCatching {
                dao.upsertSyncMetadata(
                    SyncMetadataEntity(
                        cacheKey = MarketplaceRepository.META_WORK_SYNC,
                        lastSyncedAt = prev?.lastSyncedAt ?: 0L,
                        ttlMillis = prev?.ttlMillis ?: MarketplaceRepository.HOME_CACHE_TTL_MS,
                        nextCursor = "Skipped: Waiting for Wi-Fi",
                        lastError = null
                    )
                )
            }
            return@withContext Result.success(
                workDataOf(KEY_SKIPPED_REASON to SKIPPED_WIFI_ONLY)
            )
        }

        val syncResult = marketplaceRepository.syncBackgroundSnapshot(force = forceRefresh)
        syncResult.fold(
            onSuccess = { summary ->
                runCatching { notifySavedSearchMatches() }
                Result.success(
                    workDataOf(
                        KEY_SYNCED_LISTINGS_COUNT to summary.totalCachedCount,
                        KEY_WANTED_COUNT to summary.wantedCount,
                        KEY_PRODUCTS_COUNT to summary.productsCount,
                        KEY_JOBS_COUNT to summary.jobsCount,
                        KEY_SERVICES_COUNT to summary.servicesCount,
                        KEY_PRICING_SYNCED to summary.pricingSynced,
                        KEY_PRUNED_COUNT to summary.prunedCount,
                        KEY_SYNCED_AT to summary.syncedAt
                    )
                )
            },
            onFailure = { error ->
                if (isRetryableError(error) && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(
                            KEY_ERROR_MESSAGE to (error.localizedMessage ?: "Background sync failed")
                        )
                    )
                }
            }
        )
    }

    private suspend fun notifySavedSearchMatches() {
        if (!notificationPreferencesRepository.preferences.value.generalEnabled) return
        val alertSearches = dao.getAllAlertSavedSearches()
        if (alertSearches.isEmpty()) return

        val now = System.currentTimeMillis()
        for (saved in alertSearches) {
            val savedDomain = saved.toDomain()
            val matches = marketplaceRepository.searchCachedOfflineSnapshot(savedDomain.toSearchFilter())
            val cutoff = saved.lastMatchedAt ?: saved.createdAt
            val freshMatches = matches.filter { listing ->
                listing.createdAtMillis > cutoff
            }
            if (freshMatches.isNotEmpty()) {
                val first = freshMatches.first()
                if (!notificationPreferencesRepository.canDisplayIncomingMessage(
                        kind = NotificationKind.GENERAL,
                        dedupeKey = "saved_search_${saved.id}_${first.id}"
                    )
                ) {
                    continue
                }
                val title = "New match for \"${saved.name}\""
                val body = if (freshMatches.size == 1) {
                    "${first.title} in ${first.location?.displayFull ?: "Zambia"}"
                } else {
                    "${freshMatches.size} new listings matched your saved search, starting with ${first.title}"
                }
                val data = buildMap {
                    put("type", "saved_search")
                    put("route", "explore")
                    saved.query.takeIf { it.isNotBlank() }?.let { put("query", it) }
                    saved.category?.takeIf { it.isNotBlank() }?.let { put("categoryId", it) }
                }
                MarketZedNotificationHelper.showNotification(
                    context = applicationContext,
                    title = title,
                    body = body,
                    kind = NotificationKind.GENERAL,
                    data = data,
                    notificationId = ("saved_search_" + saved.id).hashCode() and 0x7FFFFFFF
                )
                dao.updateSavedSearchLastMatched(saved.id, now)
                break
            }
        }
    }

    private fun isRetryableError(error: Throwable): Boolean {
        return when {
            error is IOException || error.cause is IOException -> true
            error is HttpSyncException ->
                error.statusCode >= 500 || error.statusCode == 429 || error.statusCode == 408
            else -> false
        }
    }

    companion object {
        const val KEY_FORCE_REFRESH = "force_refresh"
        const val KEY_RESPECT_WIFI_PREF = "respect_wifi_pref"
        const val KEY_SYNC_SOURCE = "sync_source"

        const val KEY_SYNCED_LISTINGS_COUNT = "synced_listings_count"
        const val KEY_WANTED_COUNT = "wanted_count"
        const val KEY_PRODUCTS_COUNT = "products_count"
        const val KEY_JOBS_COUNT = "jobs_count"
        const val KEY_SERVICES_COUNT = "services_count"
        const val KEY_PRICING_SYNCED = "pricing_synced"
        const val KEY_PRUNED_COUNT = "pruned_count"
        const val KEY_SYNCED_AT = "synced_at"
        const val KEY_SKIPPED_REASON = "skipped_reason"
        const val KEY_ERROR_MESSAGE = "error_message"

        const val SKIPPED_WIFI_ONLY = "wifi_only"
        const val MAX_RETRY_ATTEMPTS = 3

        internal fun hasUnmeteredNetwork(context: Context): Boolean {
            return try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return !cm.isActiveNetworkMetered
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            } catch (_: Throwable) {
                false
            }
        }
    }
}

class MarketZedWorkerFactory(
    private val marketplaceRepository: MarketplaceRepository,
    private val dao: MarketZedDao,
    private val notificationPreferencesRepository: NotificationPreferencesRepository? = null,
    private val motivationalMessageSelector: com.example.core.util.MotivationalMessageSelector? = null
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val notifRepo = notificationPreferencesRepository ?: NotificationPreferencesRepository(appContext)
        return when (workerClassName) {
            MarketplaceSyncWorker::class.java.name -> MarketplaceSyncWorker(
                appContext = appContext,
                workerParams = workerParameters,
                marketplaceRepository = marketplaceRepository,
                dao = dao,
                notificationPreferencesRepository = notifRepo
            )
            SmartEngagementWorker::class.java.name -> SmartEngagementWorker(
                appContext = appContext,
                workerParams = workerParameters,
                dao = dao,
                notificationPreferencesRepository = notifRepo
            )
            MotivationalNotificationWorker::class.java.name -> MotivationalNotificationWorker(
                appContext = appContext,
                workerParams = workerParameters,
                notificationPreferencesRepository = notifRepo,
                motivationalMessageSelector = motivationalMessageSelector
                    ?: com.example.core.util.MotivationalMessageSelector(
                        com.example.core.util.SharedPrefsMotivationalHistoryStore(appContext)
                    )
            )
            else -> null
        }
    }
}
