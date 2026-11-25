package com.example.data.notifications

import java.util.Calendar

object NotificationFrequencyGuard {
    const val MIN_INACTIVITY_MS = 36L * 60L * 60L * 1000L // 36 hours
    const val MIN_ENGAGEMENT_INTERVAL_MS = 48L * 60L * 60L * 1000L // 48 hours between reminders
    const val MIN_MOTIVATIONAL_INTERVAL_MS = 20L * 60L * 60L * 1000L // ~once per day
    const val MIN_IDLE_BEFORE_MOTIVATIONAL_MS = 4L * 60L * 60L * 1000L // skip if recently active
    const val WEEK_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L // 7 days
    const val MAX_ENGAGEMENT_PER_WEEK = 2

    fun currentHourOfDay(nowMillis: Long = System.currentTimeMillis()): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun isInQuietHours(
        hourOfDay: Int,
        startHour: Int = 22,
        endHour: Int = 7
    ): Boolean {
        val h = ((hourOfDay % 24) + 24) % 24
        val start = ((startHour % 24) + 24) % 24
        val end = ((endHour % 24) + 24) % 24
        return if (start > end) {
            h >= start || h < end
        } else if (start < end) {
            h in start until end
        } else {
            false
        }
    }

    fun isAllowedByPreferences(
        kind: NotificationKind,
        preferences: NotificationPreferences
    ): Boolean {
        return when (kind) {
            NotificationKind.GENERAL, NotificationKind.ANNOUNCEMENT -> preferences.generalEnabled
            NotificationKind.PROMOTION -> preferences.promotionalEnabled
            NotificationKind.ENGAGEMENT -> preferences.smartEngagementEnabled && preferences.promotionalEnabled
        }
    }

    fun canShowSystemNotificationNow(
        kind: NotificationKind,
        preferences: NotificationPreferences,
        hourOfDay: Int = currentHourOfDay()
    ): Boolean {
        if (!isAllowedByPreferences(kind, preferences)) return false
        if (kind.isPromotionalOrEngagement &&
            preferences.quietHoursEnabled &&
            isInQuietHours(hourOfDay, preferences.quietStartHour, preferences.quietEndHour)
        ) {
            return false
        }
        return true
    }

    fun isDuplicate(
        dedupeKey: String?,
        seenKeys: Set<String>
    ): Boolean {
        val key = dedupeKey?.trim().orEmpty()
        return key.isNotEmpty() && key in seenKeys
    }

    fun canSendSmartEngagement(
        nowMillis: Long,
        lastAppActiveAtMillis: Long,
        recentEngagementTimestamps: List<Long>,
        preferences: NotificationPreferences,
        hourOfDay: Int = currentHourOfDay(nowMillis)
    ): Boolean {
        if (!isAllowedByPreferences(NotificationKind.ENGAGEMENT, preferences)) {
            return false
        }
        if (preferences.quietHoursEnabled &&
            isInQuietHours(hourOfDay, preferences.quietStartHour, preferences.quietEndHour)
        ) {
            return false
        }
        if (lastAppActiveAtMillis > 0L && (nowMillis - lastAppActiveAtMillis) < MIN_INACTIVITY_MS) {
            return false
        }
        val lastSent = recentEngagementTimestamps.maxOrNull() ?: 0L
        if (lastSent > 0L && (nowMillis - lastSent) < MIN_ENGAGEMENT_INTERVAL_MS) {
            return false
        }
        val weeklyCount = recentEngagementTimestamps.count { ts ->
            ts in (nowMillis - WEEK_WINDOW_MS)..nowMillis
        }
        return weeklyCount < MAX_ENGAGEMENT_PER_WEEK
    }

    fun canSendMotivationalNotification(
        nowMillis: Long,
        lastAppActiveAtMillis: Long,
        lastMotivationalSentAtMillis: Long,
        preferences: NotificationPreferences,
        hourOfDay: Int = currentHourOfDay(nowMillis)
    ): Boolean {
        if (!preferences.motivationalEnabled) {
            return false
        }
        if (preferences.quietHoursEnabled &&
            isInQuietHours(hourOfDay, preferences.quietStartHour, preferences.quietEndHour)
        ) {
            return false
        }
        if (lastAppActiveAtMillis > 0L && (nowMillis - lastAppActiveAtMillis) < MIN_IDLE_BEFORE_MOTIVATIONAL_MS) {
            return false
        }
        if (lastMotivationalSentAtMillis > 0L && (nowMillis - lastMotivationalSentAtMillis) < MIN_MOTIVATIONAL_INTERVAL_MS) {
            return false
        }
        return true
    }
}
