package com.example.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val lusakaTz: TimeZone = TimeZone.getTimeZone("Africa/Lusaka")

    private val isoParsers: List<SimpleDateFormat>
        get() = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = lusakaTz
            }
        )

    fun parseIsoToMillis(iso: String?): Long? {
        val value = iso?.trim() ?: return null
        if (value.isEmpty()) return null
        for (parser in isoParsers) {
            try {
                val parsed = parser.parse(value)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun formatDateLusaka(millis: Long?): String {
        if (millis == null || millis <= 0L) return "Recently"
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US).apply {
            timeZone = lusakaTz
        }
        return formatter.format(Date(millis))
    }

    fun formatDateTimeLusaka(millis: Long?): String {
        if (millis == null || millis <= 0L) return ""
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).apply {
            timeZone = lusakaTz
        }
        return formatter.format(Date(millis))
    }

    fun formatRelativeTime(millis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (millis == null || millis <= 0L) return "Recently"
        val diff = nowMillis - millis
        if (diff < 60_000L) return "Just now"
        val minutes = diff / 60_000L
        if (minutes < 60L) return "${minutes}m ago"
        val hours = minutes / 60L
        if (hours < 24L) return "${hours}h ago"
        val days = hours / 24L
        if (days < 7L) return "${days}d ago"
        return formatDateLusaka(millis)
    }

    fun isExpired(expiresAtIso: String?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val expiryMillis = parseIsoToMillis(expiresAtIso) ?: return false
        return expiryMillis <= nowMillis
    }
}
