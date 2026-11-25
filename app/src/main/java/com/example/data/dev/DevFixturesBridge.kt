package com.example.data.dev

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.MarketZedDao
import com.example.data.model.Listing
import com.example.data.repository.LocalMappers.toCachedEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DevFixturesProvider {
    val isSupported: Boolean
    fun buildFixtures(nowMillis: Long = System.currentTimeMillis()): List<Listing>
}

class DevFixturesManager(
    context: Context,
    private val dao: MarketZedDao
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val provider: DevFixturesProvider = DevFixturesProviderFactory.create()

    val isFeatureAvailable: Boolean
        get() = BuildConfig.DEBUG && BuildConfig.MARKETZED_TEST_DATA_ENABLED && provider.isSupported

    private val _isSeeded = MutableStateFlow(
        if (isFeatureAvailable) prefs.getBoolean(KEY_DEV_FIXTURES_SEEDED, false) else false
    )
    val isSeeded: StateFlow<Boolean> = _isSeeded.asStateFlow()

    fun getFixtureCount(): Int = if (isFeatureAvailable) provider.buildFixtures().size else 0

    suspend fun seedFixtures(): Int {
        if (!isFeatureAvailable) return 0
        val now = System.currentTimeMillis()
        val fixtures = provider.buildFixtures(now)
        val grouped = fixtures.groupBy { sectionKeyForCategory(it.categoryId) }
        grouped.forEach { (sectionKey, items) ->
            dao.replaceSectionListings(
                sectionKey = sectionKey,
                listings = items.map { it.toCachedEntity(sectionKey, now) }
            )
        }
        prefs.edit().putBoolean(KEY_DEV_FIXTURES_SEEDED, true).apply()
        _isSeeded.value = true
        return fixtures.size
    }

    suspend fun clearFixtures() {
        dao.deleteDevFixtureListings()
        prefs.edit().putBoolean(KEY_DEV_FIXTURES_SEEDED, false).apply()
        _isSeeded.value = false
    }

    companion object {
        private const val PREFS_NAME = "marketzed_dev_fixtures_prefs"
        private const val KEY_DEV_FIXTURES_SEEDED = "dev_fixtures_seeded"
        const val DEV_PREFIX = "dev_fixture_"

        fun devSectionKey(liveSectionKey: String): String = "$DEV_PREFIX$liveSectionKey"

        fun sectionKeyForCategory(categoryId: String): String {
            return when (categoryId.trim().lowercase()) {
                "property" -> "${DEV_PREFIX}home_property"
                "services" -> "${DEV_PREFIX}home_services"
                "swaps" -> "${DEV_PREFIX}home_swaps"
                "wanted" -> "${DEV_PREFIX}home_wanted"
                "jobs" -> "${DEV_PREFIX}home_jobs"
                else -> "${DEV_PREFIX}home_products"
            }
        }
    }
}
