package com.example

import com.example.core.util.PriceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceFormatterTest {

    @Test
    fun `formats integer ZMW amounts with K prefix`() {
        assertEquals("K2,500", PriceFormatter.formatListingPrice(2500.0, categoryId = "marketplace"))
        assertEquals("K30", PriceFormatter.formatKwachaAmount(30.0))
    }

    @Test
    fun `formats fractional ZMW amounts to two decimal places`() {
        assertEquals("K1,250.50", PriceFormatter.formatListingPrice(1250.5, categoryId = "property"))
    }

    @Test
    fun `handles free mode and free category correctly`() {
        assertEquals("Free", PriceFormatter.formatListingPrice(0.0, priceMode = "free", categoryId = "free"))
        assertEquals("Free", PriceFormatter.formatListingPrice(null, priceMode = null, categoryId = "free"))
        assertEquals("Free", PriceFormatter.formatListingPrice(0.0, priceMode = null, categoryId = "marketplace"))
    }

    @Test
    fun `handles wanted budget and unspecified prices cleanly`() {
        assertEquals("Budget: K5,000", PriceFormatter.formatListingPrice(5000.0, categoryId = "wanted"))
        assertEquals("Budget not specified", PriceFormatter.formatListingPrice(null, categoryId = "wanted"))
        assertEquals("Budget not specified", PriceFormatter.formatListingPrice(0.0, categoryId = "wanted"))
        assertEquals("Contact for price", PriceFormatter.formatListingPrice(null, categoryId = "services"))
        assertEquals("Contact for price", PriceFormatter.formatListingPrice(-10.0, categoryId = "marketplace"))
    }

    @Test
    fun `parses valid price inputs and rejects negative or malformed inputs`() {
        assertEquals(2500.0, PriceFormatter.parsePriceInput("2,500")!!, 0.001)
        assertEquals(0.0, PriceFormatter.parsePriceInput("0")!!, 0.001)
        assertNull(PriceFormatter.parsePriceInput(""))
        assertNull(PriceFormatter.parsePriceInput("abc"))
        assertNull(PriceFormatter.parsePriceInput("-50"))
    }
}
