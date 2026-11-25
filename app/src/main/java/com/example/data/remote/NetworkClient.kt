package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.data.model.CategoryDetails
import com.example.data.model.ContactPreferences
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.model.ListingLocation
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Lenient Moshi adapters for production resilience:
 * - Nullable Double accepting numbers, numeric strings, or blank/null
 * - Listing parsing tolerant of explicit null fields, string prices, and Firestore {_seconds, _nanoseconds} timestamps
 */
internal object MarketplaceJsonAdapters {

    val lenientDoubleAdapterFactory = JsonAdapter.Factory { type: Type, _, _ ->
        if (type == Double::class.javaObjectType) {
            object : JsonAdapter<Double?>() {
                override fun fromJson(reader: JsonReader): Double? {
                    return when (reader.peek()) {
                        JsonReader.Token.NULL -> {
                            reader.nextNull<Any?>()
                            null
                        }
                        JsonReader.Token.NUMBER -> reader.nextDouble()
                        JsonReader.Token.STRING -> {
                            val raw = reader.nextString().trim().replace(",", "")
                            raw.toDoubleOrNull()?.takeIf { !it.isNaN() }
                        }
                        else -> {
                            reader.skipValue()
                            null
                        }
                    }
                }

                override fun toJson(writer: JsonWriter, value: Double?) {
                    if (value == null) writer.nullValue() else writer.value(value)
                }
            }
        } else {
            null
        }
    }

    class LenientListingAdapter(private val moshi: Moshi) {
        @FromJson
        fun fromJson(reader: JsonReader): Listing? {
            if (reader.peek() == JsonReader.Token.NULL) {
                reader.nextNull<Any?>()
                return null
            }
            var id = ""
            var ownerId: String? = null
            var sellerName: String? = null
            var sellerVerified = false
            var isPromoted = false
            var categoryId = "marketplace"
            var subcategory = ""
            var title = ""
            var description = ""
            var price: Double? = null
            var currency = "ZMW"
            var priceMode: String? = null
            var location: ListingLocation? = null
            val images = mutableListOf<ListingImage>()
            var contactPreferences: ContactPreferences? = null
            var categoryDetails: CategoryDetails? = null
            var moderationStatus: String? = null
            var availabilityStatus: String? = null
            var wantedStatus: String? = null
            var isPublic: Boolean? = true
            var reviewRequired = false
            var createdAt: String? = null
            var updatedAt: String? = null
            var publishedAt: String? = null
            var expiresAt: String? = null

            val locationAdapter = moshi.adapter(ListingLocation::class.java)
            val imageAdapter = moshi.adapter(ListingImage::class.java)
            val contactAdapter = moshi.adapter(ContactPreferences::class.java)
            val detailsAdapter = moshi.adapter(CategoryDetails::class.java)

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = readStringOrEmpty(reader)
                    "ownerId" -> ownerId = readNullableString(reader)
                    "sellerName" -> sellerName = readNullableString(reader)
                    "sellerVerified" -> sellerVerified = readBooleanOrDefault(reader, false)
                    "isPromoted" -> isPromoted = readBooleanOrDefault(reader, false)
                    "categoryId" -> categoryId = readStringOrEmpty(reader).ifBlank { "marketplace" }
                    "subcategory" -> subcategory = readStringOrEmpty(reader)
                    "title" -> title = readStringOrEmpty(reader)
                    "description" -> description = readStringOrEmpty(reader)
                    "price" -> price = readLenientDouble(reader)
                    "currency" -> currency = readStringOrEmpty(reader).ifBlank { "ZMW" }
                    "priceMode" -> priceMode = readNullableString(reader)
                    "location" -> location = readSafely(reader, locationAdapter)
                    "images" -> {
                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                readSafely(reader, imageAdapter)?.let { images.add(it) }
                            }
                            reader.endArray()
                        } else {
                            reader.skipValue()
                        }
                    }
                    "contactPreferences" -> contactPreferences = readSafely(reader, contactAdapter)
                    "categoryDetails" -> categoryDetails = readSafely(reader, detailsAdapter)
                    "moderationStatus" -> moderationStatus = readNullableString(reader)
                    "availabilityStatus" -> availabilityStatus = readNullableString(reader)
                    "wantedStatus" -> wantedStatus = readNullableString(reader)
                    "isPublic" -> isPublic = readNullableBoolean(reader)
                    "reviewRequired" -> reviewRequired = readBooleanOrDefault(reader, false)
                    "createdAt" -> createdAt = readFlexibleTimestamp(reader)
                    "updatedAt" -> updatedAt = readFlexibleTimestamp(reader)
                    "publishedAt" -> publishedAt = readFlexibleTimestamp(reader)
                    "expiresAt" -> expiresAt = readFlexibleTimestamp(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            return Listing(
                id = id,
                ownerId = ownerId,
                sellerName = sellerName,
                sellerVerified = sellerVerified,
                isPromoted = isPromoted,
                categoryId = categoryId,
                subcategory = subcategory,
                title = title,
                description = description,
                price = price,
                currency = currency,
                priceMode = priceMode,
                location = location,
                images = images,
                contactPreferences = contactPreferences,
                categoryDetails = categoryDetails,
                moderationStatus = moderationStatus,
                availabilityStatus = availabilityStatus,
                wantedStatus = wantedStatus,
                isPublic = isPublic,
                reviewRequired = reviewRequired,
                createdAt = createdAt,
                updatedAt = updatedAt,
                publishedAt = publishedAt,
                expiresAt = expiresAt
            )
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Listing?) {
            if (value == null) {
                writer.nullValue()
                return
            }
            writer.beginObject()
            writer.name("id").value(value.id)
            writer.name("ownerId").value(value.ownerId)
            writer.name("sellerName").value(value.sellerName)
            writer.name("sellerVerified").value(value.sellerVerified)
            writer.name("isPromoted").value(value.isPromoted)
            writer.name("categoryId").value(value.categoryId)
            writer.name("subcategory").value(value.subcategory)
            writer.name("title").value(value.title)
            writer.name("description").value(value.description)
            writer.name("price").value(value.price)
            writer.name("currency").value(value.currency)
            writer.name("priceMode").value(value.priceMode)
            writer.name("location")
            moshi.adapter(ListingLocation::class.java).toJson(writer, value.location)
            writer.name("images")
            val imageAdapter = moshi.adapter(ListingImage::class.java)
            writer.beginArray()
            value.images.forEach { imageAdapter.toJson(writer, it) }
            writer.endArray()
            writer.name("contactPreferences")
            moshi.adapter(ContactPreferences::class.java).toJson(writer, value.contactPreferences)
            writer.name("categoryDetails")
            moshi.adapter(CategoryDetails::class.java).toJson(writer, value.categoryDetails)
            writer.name("moderationStatus").value(value.moderationStatus)
            writer.name("availabilityStatus").value(value.availabilityStatus)
            writer.name("wantedStatus").value(value.wantedStatus)
            writer.name("isPublic").value(value.isPublic)
            writer.name("reviewRequired").value(value.reviewRequired)
            writer.name("createdAt").value(value.createdAt)
            writer.name("updatedAt").value(value.updatedAt)
            writer.name("publishedAt").value(value.publishedAt)
            writer.name("expiresAt").value(value.expiresAt)
            writer.endObject()
        }

        private fun <T> readSafely(reader: JsonReader, adapter: JsonAdapter<T>): T? {
            return when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Any?>()
                    null
                }
                JsonReader.Token.BEGIN_OBJECT -> {
                    try {
                        adapter.fromJson(reader)
                    } catch (_: Exception) {
                        null
                    }
                }
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }

        private fun readNullableString(reader: JsonReader): String? {
            return when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Any?>()
                    null
                }
                JsonReader.Token.STRING -> reader.nextString()
                JsonReader.Token.NUMBER -> reader.nextString()
                JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }

        private fun readStringOrEmpty(reader: JsonReader): String = readNullableString(reader).orEmpty()

        private fun readBooleanOrDefault(reader: JsonReader, defaultValue: Boolean): Boolean {
            return readNullableBoolean(reader) ?: defaultValue
        }

        private fun readNullableBoolean(reader: JsonReader): Boolean? {
            return when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Any?>()
                    null
                }
                JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                JsonReader.Token.STRING -> reader.nextString().trim().lowercase(Locale.ROOT).let {
                    when (it) {
                        "true" -> true
                        "false" -> false
                        else -> null
                    }
                }
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }

        private fun readLenientDouble(reader: JsonReader): Double? {
            return when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Any?>()
                    null
                }
                JsonReader.Token.NUMBER -> reader.nextDouble()
                JsonReader.Token.STRING -> reader.nextString().trim().replace(",", "").toDoubleOrNull()
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }

        private fun readFlexibleTimestamp(reader: JsonReader): String? {
            return when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Any?>()
                    null
                }
                JsonReader.Token.STRING -> reader.nextString()
                JsonReader.Token.NUMBER -> formatEpochToIso(reader.nextLong())
                JsonReader.Token.BEGIN_OBJECT -> {
                    var seconds: Long? = null
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "_seconds", "seconds" -> seconds = when (reader.peek()) {
                                JsonReader.Token.NUMBER -> reader.nextLong()
                                JsonReader.Token.STRING -> reader.nextString().toLongOrNull()
                                else -> {
                                    reader.skipValue()
                                    null
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    seconds?.let { sec -> formatEpochToIso(sec * 1000L) }
                }
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }

        private fun formatEpochToIso(epochOrSeconds: Long): String {
            val millis = if (epochOrSeconds in 1L..9_999_999_999L) epochOrSeconds * 1000L else epochOrSeconds
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return fmt.format(Date(millis))
        }
    }
}

object NetworkClient {

    val moshi: Moshi by lazy {
        val baseMoshi = Moshi.Builder()
            .add(MarketplaceJsonAdapters.lenientDoubleAdapterFactory)
            .addLast(KotlinJsonAdapterFactory())
            .build()
        baseMoshi.newBuilder()
            .add(MarketplaceJsonAdapters.LenientListingAdapter(baseMoshi))
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // Never log Authorization header or sensitive payloads
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
                redactHeader("Cookie")
            }
            builder.addInterceptor(logger)
        }
        builder.build()
    }

    val apiService: MarketZedApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.MARKETZED_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MarketZedApiService::class.java)
    }

    fun parseErrorMessage(response: Response<*>, fallback: String): String {
        return try {
            val raw = response.errorBody()?.string()
            if (raw.isNullOrBlank()) return fallback
            val adapter = moshi.adapter(ApiErrorDto::class.java)
            val parsed = adapter.fromJson(raw)
            parsed?.error?.takeIf { it.isNotBlank() }
                ?: parsed?.message?.takeIf { it.isNotBlank() }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}

class ConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isConnectedHint = MutableStateFlow(checkCurrentlyConnected())
    val isConnectedHint: StateFlow<Boolean> = _isConnectedHint.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnectedHint.value = true
        }

        override fun onLost(network: Network) {
            _isConnectedHint.value = checkCurrentlyConnected()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isConnectedHint.value =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        try {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        } catch (_: Throwable) {
            _isConnectedHint.value = checkCurrentlyConnected()
        }
    }

    fun checkCurrentlyConnected(): Boolean {
        return try {
            val cm = connectivityManager ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }
}
