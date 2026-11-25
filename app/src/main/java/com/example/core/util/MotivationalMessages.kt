package com.example.core.util

import android.content.Context
import android.content.SharedPreferences
import kotlin.random.Random

data class MotivationalMessage(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetCategoryId: String? = null
)

interface MotivationalHistoryStore {
    fun getRecentIds(): List<String>
    fun saveRecentIds(ids: List<String>)
}

class SharedPrefsMotivationalHistoryStore(context: Context) : MotivationalHistoryStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("marketzed_motivational_prefs", Context.MODE_PRIVATE)

    override fun getRecentIds(): List<String> {
        val raw = prefs.getString(KEY_RECENT_IDS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    override fun saveRecentIds(ids: List<String>) {
        prefs.edit().putString(KEY_RECENT_IDS, ids.joinToString(",")).apply()
    }

    private companion object {
        const val KEY_RECENT_IDS = "recent_message_ids"
    }
}

object MotivationalMessageCatalog {
    val messages: List<MotivationalMessage> = listOf(
        MotivationalMessage(
            id = "explore_deals",
            title = "Great local finds wait around the corner",
            subtitle = "Check fresh Marketplace listings in Lusaka, Copperbelt & beyond.",
            targetCategoryId = "marketplace"
        ),
        MotivationalMessage(
            id = "wanted_free",
            title = "Looking for something specific?",
            subtitle = "Post a Wanted request for free and let Zambian sellers reach out.",
            targetCategoryId = "wanted"
        ),
        MotivationalMessage(
            id = "turn_clutter_cash",
            title = "Unused items can become extra Kwacha",
            subtitle = "Save drafts offline anytime and publish when you are ready.",
            targetCategoryId = "marketplace"
        ),
        MotivationalMessage(
            id = "jobs_opportunities",
            title = "New opportunities open every week",
            subtitle = "Explore full-time roles, gigs, and internships across Zambia.",
            targetCategoryId = "jobs"
        ),
        MotivationalMessage(
            id = "services_trust",
            title = "Find skilled local professionals fast",
            subtitle = "Browse trusted plumbers, mechanics, designers, and tutors near you.",
            targetCategoryId = "services"
        ),
        MotivationalMessage(
            id = "swaps_smart",
            title = "Trade what you have for what you need",
            subtitle = "Discover smart exchanges in the Swaps category.",
            targetCategoryId = "swaps"
        ),
        MotivationalMessage(
            id = "property_search",
            title = "Searching for a new home or shop space?",
            subtitle = "Compare houses, rooms, and commercial spaces for rent cached for low data browsing.",
            targetCategoryId = "property"
        ),
        MotivationalMessage(
            id = "free_community",
            title = "Give or find helpful items for free",
            subtitle = "Explore the Free / Give Away section for community shares.",
            targetCategoryId = "free"
        ),
        MotivationalMessage(
            id = "save_favorites",
            title = "Save favorites to compare offline",
            subtitle = "Tap the heart on any listing to keep it handy without mobile data.",
            targetCategoryId = null
        ),
        MotivationalMessage(
            id = "provincial_reach",
            title = "Connecting buyers and sellers in all 10 provinces",
            subtitle = "Filter by province or town to find deals right in your community.",
            targetCategoryId = null
        )
    )
}

class MotivationalMessageSelector(
    private val historyStore: MotivationalHistoryStore,
    private val pool: List<MotivationalMessage> = MotivationalMessageCatalog.messages,
    private val random: Random = Random.Default
) {
    private val maxHistorySize: Int = (pool.size - 2).coerceAtLeast(1)

    fun nextMessage(excludingId: String? = null): MotivationalMessage {
        if (pool.isEmpty()) {
            return MotivationalMessage(
                id = "fallback",
                title = "Discover something new on MarketZed",
                subtitle = "Browse listings across Zambia anytime, even offline."
            )
        }
        val recent = historyStore.getRecentIds()
        val blocked = recent.toMutableSet().apply {
            if (!excludingId.isNullOrBlank()) add(excludingId)
        }
        val candidates = pool.filterNot { it.id in blocked }
        val finalCandidates = when {
            candidates.isNotEmpty() -> candidates
            !excludingId.isNullOrBlank() && pool.size > 1 -> pool.filterNot { it.id == excludingId }
            else -> pool
        }
        val picked = finalCandidates[random.nextInt(finalCandidates.size)]
        val updatedHistory = (recent.filterNot { it == picked.id } + picked.id)
            .takeLast(maxHistorySize)
        historyStore.saveRecentIds(updatedHistory)
        return picked
    }
}

object HomepageEmptyMessages {
    val messages: List<String> = listOf(
        "Every great marketplace starts with its first listing. Be the first!",
        "Have something to sell? Zambia is ready to discover it.",
        "Your next customer could be one post away.",
        "Turn what you have into an opportunity. Start listing today.",
        "Have a skill, product or service? Give it a place on MarketZed.",
        "Your first listing could help someone find exactly what they need.",
        "Start small. Get discovered. Grow with MarketZed.",
        "What are you offering today? Let Zambia see it!"
    )

    fun pickStableMessage(index: Int? = null): String {
        if (messages.isEmpty()) return "Nothing has been listed just yet. Be the first to share something with Zambia."
        val safeIdx = (index ?: Random.Default.nextInt(messages.size)).mod(messages.size)
        return messages[safeIdx]
    }
}
