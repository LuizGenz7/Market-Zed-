package com.example.data.notifications

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.NetworkClient
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class ForegroundPushNotice(
    val id: String,
    val title: String,
    val message: String,
    val kind: NotificationKind,
    val target: AppNavigationTarget?
)

class NotificationPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val listAdapter = NetworkClient.moshi.adapter<List<LocalAnnouncement>>(
        Types.newParameterizedType(List::class.java, LocalAnnouncement::class.java)
    )

    private val lock = Any()

    private val _preferences = MutableStateFlow(readPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    private val _announcements = MutableStateFlow(readAnnouncements())
    val announcements: StateFlow<List<LocalAnnouncement>> = _announcements.asStateFlow()

    private val _deletedNotificationIds = MutableStateFlow(readDeletedNotificationIds())
    val deletedNotificationIds: StateFlow<Set<String>> = _deletedNotificationIds.asStateFlow()

    private val _foregroundNotices = MutableSharedFlow<ForegroundPushNotice>(extraBufferCapacity = 8)
    val foregroundNotices: SharedFlow<ForegroundPushNotice> = _foregroundNotices.asSharedFlow()

    private fun readPreferences(): NotificationPreferences {
        return NotificationPreferences(
            generalEnabled = prefs.getBoolean(KEY_GENERAL_ENABLED, true),
            promotionalEnabled = prefs.getBoolean(KEY_PROMO_ENABLED, true),
            smartEngagementEnabled = prefs.getBoolean(KEY_SMART_ENGAGEMENT_ENABLED, true),
            motivationalEnabled = prefs.getBoolean(KEY_MOTIVATIONAL_ENABLED, true),
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, true),
            fcmToken = prefs.getString(KEY_FCM_TOKEN, null)
        )
    }

    private fun readAnnouncements(): List<LocalAnnouncement> {
        val raw = prefs.getString(KEY_ANNOUNCEMENTS_JSON, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching { listAdapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
    }

    private fun writeAnnouncements(items: List<LocalAnnouncement>) {
        val trimmed = items.take(MAX_ANNOUNCEMENTS)
        val json = runCatching { listAdapter.toJson(trimmed) }.getOrDefault("[]")
        prefs.edit().putString(KEY_ANNOUNCEMENTS_JSON, json).apply()
        _announcements.value = trimmed
    }

    private fun readDeletedNotificationIds(): Set<String> {
        val raw = prefs.getString(KEY_DELETED_NOTIFICATION_IDS, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun updateGeneralEnabled(enabled: Boolean) {
        synchronized(lock) {
            prefs.edit().putBoolean(KEY_GENERAL_ENABLED, enabled).apply()
            _preferences.value = _preferences.value.copy(generalEnabled = enabled)
        }
        syncFcmTopics()
    }

    fun updatePromotionalEnabled(enabled: Boolean) {
        synchronized(lock) {
            prefs.edit().putBoolean(KEY_PROMO_ENABLED, enabled).apply()
            _preferences.value = _preferences.value.copy(promotionalEnabled = enabled)
        }
        syncFcmTopics()
    }

    fun updateSmartEngagementEnabled(enabled: Boolean) {
        synchronized(lock) {
            prefs.edit().putBoolean(KEY_SMART_ENGAGEMENT_ENABLED, enabled).apply()
            _preferences.value = _preferences.value.copy(smartEngagementEnabled = enabled)
        }
    }

    fun updateMotivationalEnabled(enabled: Boolean) {
        synchronized(lock) {
            prefs.edit().putBoolean(KEY_MOTIVATIONAL_ENABLED, enabled).apply()
            _preferences.value = _preferences.value.copy(motivationalEnabled = enabled)
        }
    }

    fun updateQuietHoursEnabled(enabled: Boolean) {
        synchronized(lock) {
            prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
            _preferences.value = _preferences.value.copy(quietHoursEnabled = enabled)
        }
    }

    fun saveFcmToken(token: String) {
        val clean = token.trim()
        if (clean.isEmpty()) return
        synchronized(lock) {
            prefs.edit().putString(KEY_FCM_TOKEN, clean).apply()
            _preferences.value = _preferences.value.copy(fcmToken = clean)
        }
    }

    fun refreshFcmTokenAsync() {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    saveFcmToken(token)
                }
            }
        }
    }

    fun syncFcmTopics() {
        val current = _preferences.value
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            if (current.generalEnabled) {
                messaging.subscribeToTopic(TOPIC_GENERAL)
                messaging.subscribeToTopic(TOPIC_ANNOUNCEMENTS)
            } else {
                messaging.unsubscribeFromTopic(TOPIC_GENERAL)
                messaging.unsubscribeFromTopic(TOPIC_ANNOUNCEMENTS)
            }
            if (current.promotionalEnabled) {
                messaging.subscribeToTopic(TOPIC_PROMOTIONS)
            } else {
                messaging.unsubscribeFromTopic(TOPIC_PROMOTIONS)
            }
        }
    }

    fun recordAppActive(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_ACTIVE_AT, nowMillis).apply()
    }

    fun getLastAppActiveAt(): Long = prefs.getLong(KEY_LAST_ACTIVE_AT, 0L)

    fun isDuplicateDedupeKey(dedupeKey: String?): Boolean {
        val key = dedupeKey?.trim().orEmpty()
        if (key.isEmpty()) return false
        synchronized(lock) {
            return NotificationFrequencyGuard.isDuplicate(key, readSeenDedupeKeys().toSet())
        }
    }

    fun canDisplayIncomingMessage(
        kind: NotificationKind,
        dedupeKey: String?,
        isForeground: Boolean = false
    ): Boolean {
        synchronized(lock) {
            val currentPrefs = _preferences.value
            val allowed = if (isForeground) {
                NotificationFrequencyGuard.isAllowedByPreferences(kind, currentPrefs)
            } else {
                NotificationFrequencyGuard.canShowSystemNotificationNow(kind, currentPrefs)
            }
            if (!allowed) {
                return false
            }
            val cleanKey = dedupeKey?.trim().orEmpty()
            if (cleanKey.isNotEmpty()) {
                val seen = readSeenDedupeKeys()
                if (NotificationFrequencyGuard.isDuplicate(cleanKey, seen.toSet())) {
                    return false
                }
                appendSeenDedupeKeyLocked(cleanKey, seen)
            }
            return true
        }
    }

    fun canSendSmartEngagement(nowMillis: Long = System.currentTimeMillis()): Boolean {
        synchronized(lock) {
            return NotificationFrequencyGuard.canSendSmartEngagement(
                nowMillis = nowMillis,
                lastAppActiveAtMillis = getLastAppActiveAt(),
                recentEngagementTimestamps = readEngagementTimestamps(),
                preferences = _preferences.value
            )
        }
    }

    fun recordSmartEngagementSent(
        dedupeKey: String?,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        synchronized(lock) {
            val cutoff = nowMillis - NotificationFrequencyGuard.WEEK_WINDOW_MS
            val updated = (readEngagementTimestamps().filter { it >= cutoff } + nowMillis)
            prefs.edit()
                .putString(KEY_ENGAGEMENT_TIMESTAMPS, updated.joinToString(","))
                .apply()

            val cleanKey = dedupeKey?.trim().orEmpty()
            if (cleanKey.isNotEmpty()) {
                val seen = readSeenDedupeKeys()
                appendSeenDedupeKeyLocked(cleanKey, seen)
            }
        }
    }

    fun canSendMotivationalNotification(nowMillis: Long = System.currentTimeMillis()): Boolean {
        synchronized(lock) {
            return NotificationFrequencyGuard.canSendMotivationalNotification(
                nowMillis = nowMillis,
                lastAppActiveAtMillis = getLastAppActiveAt(),
                lastMotivationalSentAtMillis = prefs.getLong(KEY_LAST_MOTIVATIONAL_SENT_AT, 0L),
                preferences = _preferences.value
            )
        }
    }

    fun recordMotivationalSent(nowMillis: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            prefs.edit().putLong(KEY_LAST_MOTIVATIONAL_SENT_AT, nowMillis).apply()
        }
    }

    fun addAnnouncement(announcement: LocalAnnouncement) {
        synchronized(lock) {
            val existing = _announcements.value
            if (existing.any { it.id == announcement.id }) return
            writeAnnouncements(listOf(announcement) + existing)
        }
    }

    fun markAnnouncementRead(id: String) {
        synchronized(lock) {
            val updated = _announcements.value.map {
                if (it.id == id) it.copy(read = true) else it
            }
            writeAnnouncements(updated)
        }
    }

    fun markAllAnnouncementsRead() {
        synchronized(lock) {
            val updated = _announcements.value.map { it.copy(read = true) }
            writeAnnouncements(updated)
        }
    }

    fun deleteAnnouncement(id: String) {
        synchronized(lock) {
            val updated = _announcements.value.filterNot { it.id == id }
            writeAnnouncements(updated)
        }
    }

    fun recordDeletedNotificationId(id: String) {
        val clean = id.trim()
        if (clean.isEmpty()) return
        synchronized(lock) {
            val updated = (_deletedNotificationIds.value + clean).toList().takeLast(MAX_DELETED_IDS).toSet()
            prefs.edit().putString(KEY_DELETED_NOTIFICATION_IDS, updated.joinToString("\n")).apply()
            _deletedNotificationIds.value = updated
        }
    }

    fun emitForegroundNotice(notice: ForegroundPushNotice) {
        _foregroundNotices.tryEmit(notice)
    }

    private fun readSeenDedupeKeys(): List<String> {
        val raw = prefs.getString(KEY_SEEN_DEDUPE_KEYS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    private fun appendSeenDedupeKeyLocked(key: String, existing: List<String>) {
        val updated = (existing.filterNot { it == key } + key).takeLast(MAX_DEDUPE_KEYS)
        prefs.edit().putString(KEY_SEEN_DEDUPE_KEYS, updated.joinToString("\n")).apply()
    }

    private fun readEngagementTimestamps(): List<Long> {
        val raw = prefs.getString(KEY_ENGAGEMENT_TIMESTAMPS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    companion object {
        private const val PREFS_NAME = "marketzed_notification_prefs"
        private const val KEY_GENERAL_ENABLED = "general_enabled"
        private const val KEY_PROMO_ENABLED = "promotional_enabled"
        private const val KEY_SMART_ENGAGEMENT_ENABLED = "smart_engagement_enabled"
        private const val KEY_MOTIVATIONAL_ENABLED = "motivational_enabled"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_LAST_ACTIVE_AT = "last_active_at"
        private const val KEY_LAST_MOTIVATIONAL_SENT_AT = "last_motivational_sent_at"
        private const val KEY_ENGAGEMENT_TIMESTAMPS = "engagement_timestamps"
        private const val KEY_SEEN_DEDUPE_KEYS = "seen_dedupe_keys"
        private const val KEY_ANNOUNCEMENTS_JSON = "announcements_json"
        private const val KEY_DELETED_NOTIFICATION_IDS = "deleted_notification_ids"

        private const val MAX_DEDUPE_KEYS = 100
        private const val MAX_ANNOUNCEMENTS = 30
        private const val MAX_DELETED_IDS = 200

        const val TOPIC_GENERAL = "marketzed_general"
        const val TOPIC_ANNOUNCEMENTS = "marketzed_announcements"
        const val TOPIC_PROMOTIONS = "marketzed_promotions"
    }
}
