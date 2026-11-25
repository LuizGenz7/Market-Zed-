package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.local.MarketZedDao
import com.example.data.notifications.MarketZedNotificationHelper
import com.example.data.notifications.NotificationKind
import com.example.data.notifications.NotificationPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodic worker that delivers occasional, frequency-capped engagement notifications
 * using real unpublished drafts, cached sponsored listings, or discovery prompts
 * without spamming active users or violating quiet hours.
 */
class SmartEngagementWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val dao: MarketZedDao,
    private val notificationPreferencesRepository: NotificationPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!notificationPreferencesRepository.canSendSmartEngagement()) {
            return@withContext Result.success()
        }
        if (!MarketZedNotificationHelper.canPostSystemNotifications(applicationContext)) {
            return@withContext Result.success()
        }

        val recentDraft = runCatching { dao.getMostRecentDraft() }.getOrNull()
            ?.takeIf { draft ->
                (draft.title.isNotBlank() || draft.description.isNotBlank()) &&
                    !MarketZedCatalogue.isProhibitedLandOrPropertySale(
                        categoryId = draft.categoryId,
                        subcategory = draft.subcategory,
                        title = draft.title,
                        description = draft.description,
                        priceMode = null
                    )
            }
        if (recentDraft != null) {
            val draftDedupeKey = "engage_draft_${recentDraft.localUuid}"
            if (!notificationPreferencesRepository.isDuplicateDedupeKey(draftDedupeKey)) {
                val title = "Finish your MarketZed draft"
                val draftLabel = recentDraft.title.trim().ifBlank {
                    MarketZedCatalogue.categoryLabel(recentDraft.categoryId)
                }
                val body = "Continue \"$draftLabel\" whenever you're ready to publish."
                val payload = mapOf(
                    "notificationId" to draftDedupeKey,
                    "route" to "account",
                    "type" to "engagement",
                    "title" to title,
                    "body" to body
                )
                val posted = MarketZedNotificationHelper.showNotification(
                    context = applicationContext,
                    title = title,
                    body = body,
                    kind = NotificationKind.ENGAGEMENT,
                    data = payload,
                    notificationId = draftDedupeKey.hashCode()
                )
                if (posted) {
                    notificationPreferencesRepository.recordSmartEngagementSent(draftDedupeKey)
                    return@withContext Result.success()
                }
            }
        }

        val cachedListings = runCatching { dao.getAllCachedListingsSnapshot() }
            .getOrDefault(emptyList())
            .filter {
                !it.availabilityStatus.equals("unavailable", ignoreCase = true) &&
                    !it.wantedStatus.equals("fulfilled", ignoreCase = true) &&
                    it.title.isNotBlank() &&
                    !MarketZedCatalogue.isProhibitedLandOrPropertySale(
                        categoryId = it.categoryId,
                        subcategory = it.subcategory,
                        title = it.title,
                        description = it.description,
                        priceMode = it.priceMode
                    )
            }

        val candidate = cachedListings
            .sortedWith(
                compareByDescending<com.example.data.local.CachedListingEntity> { it.isPromoted }
                    .thenByDescending { it.cachedAt }
            )
            .firstOrNull { item ->
                !notificationPreferencesRepository.isDuplicateDedupeKey("engage_listing_${item.id}")
            }

        if (candidate != null) {
            val dedupeKey = "engage_listing_${candidate.id}"
            val priceSummary = PriceFormatter.formatListingPrice(
                price = candidate.price,
                priceMode = candidate.priceMode,
                categoryId = candidate.categoryId
            )
            val title = when {
                candidate.isPromoted -> "Sponsored on MarketZed"
                candidate.categoryId.equals("jobs", ignoreCase = true) ->
                    "New job in ${candidate.town.ifBlank { candidate.province }}"
                else -> "Fresh listing in ${candidate.town.ifBlank { candidate.province }}"
            }
            val body = "${candidate.title} • $priceSummary"
            val payload = mapOf(
                "notificationId" to dedupeKey,
                "listingId" to candidate.id,
                "categoryId" to candidate.categoryId,
                "type" to "engagement",
                "title" to title,
                "body" to body
            )
            val posted = MarketZedNotificationHelper.showNotification(
                context = applicationContext,
                title = title,
                body = body,
                kind = NotificationKind.ENGAGEMENT,
                data = payload,
                notificationId = dedupeKey.hashCode()
            )
            if (posted) {
                notificationPreferencesRepository.recordSmartEngagementSent(dedupeKey)
            }
        } else {
            val weekBucket = System.currentTimeMillis() / (7L * 24L * 60L * 60L * 1000L)
            val dedupeKey = "engage_discovery_$weekBucket"
            if (!notificationPreferencesRepository.isDuplicateDedupeKey(dedupeKey)) {
                val title = "Explore fresh listings on MarketZed"
                val body = "Discover new Marketplace deals, rental properties, swaps & jobs near you."
                val payload = mapOf(
                    "notificationId" to dedupeKey,
                    "route" to "explore",
                    "type" to "engagement",
                    "title" to title,
                    "body" to body
                )
                val posted = MarketZedNotificationHelper.showNotification(
                    context = applicationContext,
                    title = title,
                    body = body,
                    kind = NotificationKind.ENGAGEMENT,
                    data = payload,
                    notificationId = dedupeKey.hashCode()
                )
                if (posted) {
                    notificationPreferencesRepository.recordSmartEngagementSent(dedupeKey)
                }
            }
        }

        Result.success()
    }
}
