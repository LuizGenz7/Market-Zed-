package com.example.data.repository

import android.content.Context
import com.example.data.model.AppNotification
import com.example.data.model.Listing
import com.example.data.model.PaymentRecord
import com.example.data.model.PromotionPricing
import com.example.data.model.PromotionRecord
import com.example.data.model.UserProfile
import com.example.data.remote.NetworkClient
import com.squareup.moshi.Types

interface LocalAccountCacheStore {
    fun getProfile(userScope: String): UserProfile?
    fun saveProfile(userScope: String, profile: UserProfile)

    fun getMyListings(userScope: String): List<Listing>
    fun saveMyListings(userScope: String, items: List<Listing>)

    fun getNotifications(userScope: String): List<AppNotification>
    fun saveNotifications(userScope: String, items: List<AppNotification>)

    fun getPayments(userScope: String): List<PaymentRecord>
    fun savePayments(userScope: String, items: List<PaymentRecord>)

    fun getPromotions(userScope: String): List<PromotionRecord>
    fun savePromotions(userScope: String, items: List<PromotionRecord>)

    fun getPromotionPricing(): PromotionPricing?
    fun savePromotionPricing(pricing: PromotionPricing)

    fun getLocalFreeWantedPostUsed(userScope: String): Boolean
    fun markLocalFreeWantedPostUsed(userScope: String)

    fun clearUserScope(userScope: String)
}

class InMemoryLocalAccountCache : LocalAccountCacheStore {
    private val profiles = mutableMapOf<String, UserProfile>()
    private val listings = mutableMapOf<String, List<Listing>>()
    private val notifications = mutableMapOf<String, List<AppNotification>>()
    private val payments = mutableMapOf<String, List<PaymentRecord>>()
    private val promotions = mutableMapOf<String, List<PromotionRecord>>()
    private val freeWantedUsedScopes = mutableSetOf<String>()
    private var pricing: PromotionPricing? = null

    override fun getProfile(userScope: String): UserProfile? = profiles[userScope]
    override fun saveProfile(userScope: String, profile: UserProfile) {
        profiles[userScope] = profile
    }

    override fun getMyListings(userScope: String): List<Listing> = listings[userScope].orEmpty()
    override fun saveMyListings(userScope: String, items: List<Listing>) {
        listings[userScope] = items
    }

    override fun getNotifications(userScope: String): List<AppNotification> = notifications[userScope].orEmpty()
    override fun saveNotifications(userScope: String, items: List<AppNotification>) {
        notifications[userScope] = items
    }

    override fun getPayments(userScope: String): List<PaymentRecord> = payments[userScope].orEmpty()
    override fun savePayments(userScope: String, items: List<PaymentRecord>) {
        payments[userScope] = items
    }

    override fun getPromotions(userScope: String): List<PromotionRecord> = promotions[userScope].orEmpty()
    override fun savePromotions(userScope: String, items: List<PromotionRecord>) {
        promotions[userScope] = items
    }

    override fun getPromotionPricing(): PromotionPricing? = pricing
    override fun savePromotionPricing(pricing: PromotionPricing) {
        this.pricing = pricing
    }

    override fun getLocalFreeWantedPostUsed(userScope: String): Boolean =
        freeWantedUsedScopes.contains(userScope)

    override fun markLocalFreeWantedPostUsed(userScope: String) {
        freeWantedUsedScopes.add(userScope)
    }

    override fun clearUserScope(userScope: String) {
        profiles.remove(userScope)
        listings.remove(userScope)
        notifications.remove(userScope)
        payments.remove(userScope)
        promotions.remove(userScope)
    }
}

class SharedPrefsLocalAccountCache(context: Context) : LocalAccountCacheStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = NetworkClient.moshi

    private val profileAdapter = moshi.adapter(UserProfile::class.java)
    private val listingListAdapter = moshi.adapter<List<Listing>>(
        Types.newParameterizedType(List::class.java, Listing::class.java)
    )
    private val notificationListAdapter = moshi.adapter<List<AppNotification>>(
        Types.newParameterizedType(List::class.java, AppNotification::class.java)
    )
    private val paymentListAdapter = moshi.adapter<List<PaymentRecord>>(
        Types.newParameterizedType(List::class.java, PaymentRecord::class.java)
    )
    private val promotionListAdapter = moshi.adapter<List<PromotionRecord>>(
        Types.newParameterizedType(List::class.java, PromotionRecord::class.java)
    )
    private val promotionPricingAdapter = moshi.adapter(PromotionPricing::class.java)

    override fun getProfile(userScope: String): UserProfile? =
        readObject(keyProfile(userScope), profileAdapter)

    override fun saveProfile(userScope: String, profile: UserProfile) {
        writeObject(keyProfile(userScope), profileAdapter, profile)
    }

    override fun getMyListings(userScope: String): List<Listing> =
        readObject(keyMyListings(userScope), listingListAdapter).orEmpty()

    override fun saveMyListings(userScope: String, items: List<Listing>) {
        writeObject(keyMyListings(userScope), listingListAdapter, items)
    }

    override fun getNotifications(userScope: String): List<AppNotification> =
        readObject(keyNotifications(userScope), notificationListAdapter).orEmpty()

    override fun saveNotifications(userScope: String, items: List<AppNotification>) {
        writeObject(keyNotifications(userScope), notificationListAdapter, items)
    }

    override fun getPayments(userScope: String): List<PaymentRecord> =
        readObject(keyPayments(userScope), paymentListAdapter).orEmpty()

    override fun savePayments(userScope: String, items: List<PaymentRecord>) {
        writeObject(keyPayments(userScope), paymentListAdapter, items)
    }

    override fun getPromotions(userScope: String): List<PromotionRecord> =
        readObject(keyPromotions(userScope), promotionListAdapter).orEmpty()

    override fun savePromotions(userScope: String, items: List<PromotionRecord>) {
        writeObject(keyPromotions(userScope), promotionListAdapter, items)
    }

    override fun getPromotionPricing(): PromotionPricing? =
        readObject(KEY_PROMOTION_PRICING, promotionPricingAdapter)

    override fun savePromotionPricing(pricing: PromotionPricing) {
        writeObject(KEY_PROMOTION_PRICING, promotionPricingAdapter, pricing)
    }

    override fun getLocalFreeWantedPostUsed(userScope: String): Boolean =
        prefs.getBoolean(keyFreeWantedUsed(userScope), false)

    override fun markLocalFreeWantedPostUsed(userScope: String) {
        prefs.edit().putBoolean(keyFreeWantedUsed(userScope), true).apply()
    }

    override fun clearUserScope(userScope: String) {
        prefs.edit()
            .remove(keyProfile(userScope))
            .remove(keyMyListings(userScope))
            .remove(keyNotifications(userScope))
            .remove(keyPayments(userScope))
            .remove(keyPromotions(userScope))
            .apply()
    }

    private fun <T> readObject(key: String, adapter: com.squareup.moshi.JsonAdapter<T>): T? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { adapter.fromJson(raw) }.getOrNull()
    }

    private fun <T> writeObject(key: String, adapter: com.squareup.moshi.JsonAdapter<T>, value: T) {
        runCatching { adapter.toJson(value) }.getOrNull()?.let { json ->
            prefs.edit().putString(key, json).apply()
        }
    }

    private fun keyProfile(scope: String) = "profile_$scope"
    private fun keyMyListings(scope: String) = "my_listings_$scope"
    private fun keyNotifications(scope: String) = "notifications_$scope"
    private fun keyPayments(scope: String) = "payments_$scope"
    private fun keyPromotions(scope: String) = "promotions_$scope"
    private fun keyFreeWantedUsed(scope: String) = "free_wanted_used_$scope"

    companion object {
        private const val PREFS_NAME = "marketzed_account_cache_prefs"
        private const val KEY_PROMOTION_PRICING = "promotion_pricing"
    }
}
