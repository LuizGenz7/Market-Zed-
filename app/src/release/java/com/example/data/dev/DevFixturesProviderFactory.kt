package com.example.data.dev

import com.example.data.model.Listing

object DevFixturesProviderFactory {
    fun create(): DevFixturesProvider = object : DevFixturesProvider {
        override val isSupported: Boolean = false
        override fun buildFixtures(nowMillis: Long): List<Listing> = emptyList()
    }
}
