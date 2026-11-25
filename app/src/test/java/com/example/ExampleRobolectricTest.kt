package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalogue.MarketZedCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExampleRobolectricTest {

    @Test
    fun `read MarketZed app name and ownership version from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MarketZed", appName)
        assertEquals("2026-11-16-v1", MarketZedCatalogue.OWNERSHIP_DECLARATION_VERSION)
        assertEquals(7, MarketZedCatalogue.categories.size)
        assertEquals(10, MarketZedCatalogue.provinces.size)
        assertTrue(MarketZedCatalogue.categories.any { it.id == "wanted" && !it.requiresImage })
    }
}
