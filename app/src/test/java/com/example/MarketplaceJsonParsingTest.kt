package com.example

import com.example.data.remote.NetworkClient
import com.example.data.remote.SearchResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceJsonParsingTest {

    @Test
    fun `lenient listing adapter handles nulls string prices and firestore timestamps`() {
        val json = """
            {
              "items": [
                {
                  "id": "list_1",
                  "sellerVerified": null,
                  "isPromoted": true,
                  "categoryId": "marketplace",
                  "subcategory": null,
                  "title": "Samsung Galaxy A54",
                  "description": "Clean condition",
                  "price": "4500",
                  "currency": null,
                  "location": { "province": "Lusaka", "town": "Lusaka" },
                  "images": null,
                  "moderationStatus": "live_needs_review",
                  "availabilityStatus": "available",
                  "isPublic": true,
                  "createdAt": { "_seconds": 1732147200, "_nanoseconds": 0 }
                },
                {
                  "id": "list_2",
                  "categoryId": "wanted",
                  "title": "Looking for laptop",
                  "price": "",
                  "createdAt": "2026-11-20T10:00:00Z"
                }
              ],
              "nextCursor": null
            }
        """.trimIndent()

        val adapter = NetworkClient.moshi.adapter(SearchResponseDto::class.java)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.items.size)

        val first = parsed.items[0]
        assertEquals("list_1", first.id)
        assertEquals(4500.0, first.price!!, 0.01)
        assertEquals("ZMW", first.currency)
        assertEquals(false, first.sellerVerified)
        assertTrue(first.images.isEmpty())
        assertTrue(first.createdAtMillis > 0L)
        assertTrue(first.isEligiblePublic)

        val second = parsed.items[1]
        assertEquals("list_2", second.id)
        assertNull(second.price)
        assertTrue(second.isWanted)
        assertTrue(second.createdAtMillis > 0L)
    }
}
