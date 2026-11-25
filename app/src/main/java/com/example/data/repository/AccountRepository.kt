package com.example.data.repository

import android.content.Context
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthTokenResult
import com.example.data.auth.SessionValidationResult
import com.example.data.model.AppNotification
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.model.PaymentRecord
import com.example.data.model.PromotionPricing
import com.example.data.model.PromotionRecord
import com.example.data.model.UserProfile
import com.example.data.remote.ConnectivityObserver
import com.example.data.remote.CreatePaymentRequestDto
import com.example.data.remote.ManageListingRequestDto
import com.example.data.remote.MarketZedApiService
import com.example.data.remote.MarkNotificationReadRequestDto
import com.example.data.remote.NetworkClient
import com.example.data.remote.PaymentResponseDto
import com.example.data.remote.PaymentStatusRequestDto
import com.example.data.remote.ProfileUpdateRequestDto
import com.example.data.remote.ReportListingRequestDto
import com.example.data.remote.ReviseListingRequestDto
import com.example.data.remote.SubmitListingRequestDto
import com.example.data.remote.SubmitListingResponseDto
import com.example.data.remote.TestimonialRequestDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

enum class AccountErrorKind {
    OFFLINE,
    NETWORK,
    SESSION_EXPIRED,
    PERMISSION_DENIED,
    SERVER
}

class AccountFetchException(
    val kind: AccountErrorKind,
    override val message: String
) : Exception(message)

sealed class MutationResult<out T> {
    data class Success<T>(val data: T) : MutationResult<T>()
    data class Error(val message: String, val isOffline: Boolean = false) : MutationResult<Nothing>()
    data class AmbiguousTimeout(val message: String) : MutationResult<Nothing>()
}

class AccountRepository(
    private val context: Context,
    private val api: MarketZedApiService,
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver
) {

    private suspend fun <T> executeProtectedGet(
        call: suspend (bearer: String) -> Response<T>,
        fallbackError: String
    ): Result<T> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return Result.failure(
                AccountFetchException(
                    AccountErrorKind.OFFLINE,
                    "You are offline. Showing saved data where available."
                )
            )
        }

        val initialToken = when (val tokenRes = authRepository.getIdTokenResult(false)) {
            is AuthTokenResult.Token -> tokenRes.jwt
            AuthTokenResult.NoUser -> return Result.failure(
                AccountFetchException(AccountErrorKind.SESSION_EXPIRED, "Please sign in to continue.")
            )
            is AuthTokenResult.SessionInvalidated -> return Result.failure(
                AccountFetchException(AccountErrorKind.SESSION_EXPIRED, tokenRes.message)
            )
            is AuthTokenResult.OfflineOrTransient -> return Result.failure(
                AccountFetchException(AccountErrorKind.NETWORK, tokenRes.message)
            )
        }

        return try {
            var response = call("Bearer $initialToken")
            if (response.code() == 401) {
                when (val validation = authRepository.validateAndRefreshSession(forceReload = true)) {
                    is SessionValidationResult.Invalidated -> {
                        return Result.failure(
                            AccountFetchException(AccountErrorKind.SESSION_EXPIRED, validation.reason)
                        )
                    }
                    is SessionValidationResult.NoSession -> {
                        return Result.failure(
                            AccountFetchException(
                                AccountErrorKind.SESSION_EXPIRED,
                                "Please sign in to continue."
                            )
                        )
                    }
                    is SessionValidationResult.OfflineCachedValid -> {
                        return Result.failure(
                            AccountFetchException(
                                AccountErrorKind.NETWORK,
                                "Network error while refreshing your session. Showing saved data."
                            )
                        )
                    }
                    SessionValidationResult.Valid -> {
                        when (val refreshed = authRepository.getIdTokenResult(true)) {
                            is AuthTokenResult.Token -> response = call("Bearer ${refreshed.jwt}")
                            is AuthTokenResult.SessionInvalidated -> return Result.failure(
                                AccountFetchException(AccountErrorKind.SESSION_EXPIRED, refreshed.message)
                            )
                            is AuthTokenResult.OfflineOrTransient -> return Result.failure(
                                AccountFetchException(AccountErrorKind.NETWORK, refreshed.message)
                            )
                            AuthTokenResult.NoUser -> return Result.failure(
                                AccountFetchException(AccountErrorKind.SESSION_EXPIRED, "Please sign in to continue.")
                            )
                        }
                    }
                }
            }

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawErr = NetworkClient.parseErrorMessage(response, fallbackError)
                when (response.code()) {
                    401 -> Result.failure(
                        AccountFetchException(
                            AccountErrorKind.SESSION_EXPIRED,
                            sanitizeUserSessionMessage(rawErr)
                        )
                    )
                    403 -> Result.failure(
                        AccountFetchException(
                            AccountErrorKind.PERMISSION_DENIED,
                            sanitizeUserPermissionMessage(rawErr)
                        )
                    )
                    else -> Result.failure(
                        AccountFetchException(
                            AccountErrorKind.SERVER,
                            sanitizeUserSessionMessage(rawErr, defaultFallback = fallbackError)
                        )
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            Result.failure(
                AccountFetchException(
                    AccountErrorKind.NETWORK,
                    "Connection timed out. Check your internet and try again."
                )
            )
        } catch (_: IOException) {
            Result.failure(
                AccountFetchException(
                    AccountErrorKind.OFFLINE,
                    "You appear to be offline. Connect to refresh your account data."
                )
            )
        } catch (e: Exception) {
            Result.failure(
                AccountFetchException(
                    AccountErrorKind.SERVER,
                    e.localizedMessage ?: fallbackError
                )
            )
        }
    }

    private fun sanitizeUserSessionMessage(
        raw: String,
        defaultFallback: String = "Your sign-in session expired or could not be verified. Please sign in again."
    ): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return defaultFallback
        // Prevent misleading backend admin-session phrasing from appearing on normal user routes
        if (trimmed.contains("admin session", ignoreCase = true) ||
            trimmed.contains("admin", ignoreCase = true) && trimmed.contains("expired", ignoreCase = true)
        ) {
            return "Your sign-in session expired or could not be verified. Please sign in again."
        }
        return trimmed
    }

    private fun sanitizeUserPermissionMessage(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.contains("admin", ignoreCase = true)) {
            return "You do not have permission to access this resource with the current account."
        }
        return trimmed
    }

    suspend fun getProfile(): Result<UserProfile> =
        executeProtectedGet({ api.getProfile(it) }, "Could not load profile.")

    suspend fun updateProfile(request: ProfileUpdateRequestDto): MutationResult<UserProfile> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — profile changes require an internet connection.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in to save your profile.")
        return try {
            val response = api.updateProfile("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                MutationResult.Success(response.body()!!)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Failed to update profile."))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout("Request timed out. Refresh your profile before trying again.")
        } catch (_: IOException) {
            MutationResult.Error("Network error while updating profile.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Failed to update profile.")
        }
    }

    suspend fun getMyListings(): Result<List<Listing>> =
        executeProtectedGet({ api.getMyListings(it) }, "Could not load your listings.")
            .map { it.items }

    suspend fun getMyPayments(): Result<List<PaymentRecord>> =
        executeProtectedGet({ api.getMyPayments(it) }, "Could not load payment history.")
            .map { it.items }

    suspend fun getMyPromotions(): Result<List<PromotionRecord>> =
        executeProtectedGet({ api.getMyPromotions(it) }, "Could not load promotions.")
            .map { it.items }

    suspend fun getPromotionPricing(): Result<PromotionPricing> {
        return try {
            val response = api.getPromotionPricing()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(NetworkClient.parseErrorMessage(response, "Could not load promotion prices.")))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Offline — promotion prices require an internet connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(): Result<List<AppNotification>> =
        executeProtectedGet({ api.getNotifications(it) }, "Could not load notifications.")
            .map { it.items }

    suspend fun markNotificationRead(notificationId: String): MutationResult<Unit> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — cannot update notification state.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Sign in required.")
        return try {
            val response = api.markNotificationRead("Bearer $token", MarkNotificationReadRequestDto(notificationId))
            if (response.isSuccessful) {
                MutationResult.Success(Unit)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Failed to mark notification read."))
            }
        } catch (_: IOException) {
            MutationResult.Error("Network error.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Error updating notification.")
        }
    }

    suspend fun createK30ActivationPayment(
        accountNumber: String,
        payerType: String
    ): MutationResult<PaymentResponseDto> {
        return createPayment(
            CreatePaymentRequestDto(
                purpose = "account_verification",
                accountNumber = accountNumber,
                payerType = payerType
            )
        )
    }

    suspend fun createPromotionPayment(
        listingId: String,
        durationDays: Int,
        accountNumber: String,
        payerType: String,
        campaignFormat: String? = null
    ): MutationResult<PaymentResponseDto> {
        return createPayment(
            CreatePaymentRequestDto(
                purpose = "promotion",
                listingId = listingId,
                promotionId = listingId,
                durationDays = durationDays,
                accountNumber = accountNumber,
                payerType = payerType,
                campaignFormat = campaignFormat
            )
        )
    }

    private suspend fun createPayment(body: CreatePaymentRequestDto): MutationResult<PaymentResponseDto> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — payments cannot be initiated offline.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in before making a payment.")
        // NEVER auto-retry payment creation to avoid duplicate charges
        return try {
            val response = api.createPayment("Bearer $token", body)
            if (response.isSuccessful && response.body() != null) {
                MutationResult.Success(response.body()!!)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Could not start mobile money payment."))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout(
                "Payment request timed out. Check Payment History or tap Check Status before creating another payment."
            )
        } catch (_: IOException) {
            MutationResult.Error("Network interrupted. Check Payment History before retrying.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Payment request failed.")
        }
    }

    suspend fun checkPaymentStatus(paymentId: String): MutationResult<PaymentResponseDto> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — connect to check server payment status.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Sign in required.")
        return try {
            val response = api.checkPaymentStatus("Bearer $token", PaymentStatusRequestDto(paymentId))
            if (response.isSuccessful && response.body() != null) {
                MutationResult.Success(response.body()!!)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Could not verify payment status."))
            }
        } catch (_: IOException) {
            MutationResult.Error("Network error checking payment status.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Status check failed.")
        }
    }

    suspend fun uploadImage(localFile: File): MutationResult<ListingImage> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — image uploads require an internet connection.", isOffline = true)
        }
        if (!localFile.exists() || !localFile.isFile) {
            return MutationResult.Error("Selected image file could not be found on this device.")
        }
        val maxBytes = 8L * 1024L * 1024L
        if (localFile.length() > maxBytes) {
            return MutationResult.Error("Image exceeds the 8 MB size limit.")
        }
        val mime = detectImageMime(localFile)
            ?: return MutationResult.Error("Only JPG, PNG, or WebP images are supported.")

        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in to upload photos.")

        return try {
            val reqBody = localFile.asRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", localFile.name, reqBody)
            val response = api.uploadImage("Bearer $token", part)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                MutationResult.Success(
                    ListingImage(
                        url = dto.url,
                        thumbUrl = dto.thumbUrl,
                        publicId = dto.publicId,
                        width = dto.width,
                        height = dto.height,
                        format = dto.format
                    )
                )
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Image upload failed (${response.code()})."))
            }
        } catch (_: IOException) {
            MutationResult.Error("Upload interrupted by network error. Tap retry on this photo.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Upload failed.")
        }
    }

    private fun detectImageMime(file: File): String? {
        return try {
            val header = ByteArray(12)
            file.inputStream().use { input ->
                val read = input.read(header)
                if (read < 4) return null
            }
            when {
                // JPEG: FF D8 FF
                header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte() -> "image/jpeg"
                // PNG: 89 50 4E 47
                header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() -> "image/png"
                // WebP: RIFF....WEBP
                header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                    header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() && header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte() -> "image/webp"
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun submitListing(payload: SubmitListingRequestDto): MutationResult<SubmitListingResponseDto> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — your draft is saved on this device. Connect to publish.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in to publish.")
        // Never blindly retry listing submit after ambiguous timeout
        return try {
            val response = api.submitListing("Bearer $token", payload)
            if (response.isSuccessful && response.body() != null) {
                MutationResult.Success(response.body()!!)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Could not publish listing (${response.code()})."))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout(
                "Could not confirm whether your listing finished publishing. Check My Listings before submitting again so you don't create a duplicate."
            )
        } catch (_: IOException) {
            MutationResult.Error("Connection lost. Your draft is safely preserved on this device.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Publishing error.")
        }
    }

    suspend fun manageListing(listingId: String, action: String): MutationResult<Unit> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — listing status cannot be changed offline.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in.")
        return try {
            val response = api.manageListing("Bearer $token", ManageListingRequestDto(listingId, action))
            if (response.isSuccessful) {
                MutationResult.Success(Unit)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Could not update listing status."))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout("Update timed out. Pull to refresh My Listings to confirm status.")
        } catch (_: IOException) {
            MutationResult.Error("Network error.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Action failed.")
        }
    }

    suspend fun reviseListing(listingId: String, changes: Map<String, Any?>): MutationResult<Unit> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — edits require an internet connection.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in.")
        return try {
            val response = api.reviseListing("Bearer $token", ReviseListingRequestDto(listingId, changes))
            if (response.isSuccessful) {
                MutationResult.Success(Unit)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Could not save listing changes."))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout("Edit timed out. Check My Listings to confirm whether changes were saved.")
        } catch (_: IOException) {
            MutationResult.Error("Network error.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Revision failed.")
        }
    }

    suspend fun reportListing(listingId: String, reason: String, details: String?): MutationResult<Unit> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — reports require an internet connection.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in to report a listing.")
        return try {
            val response = api.reportListing(
                "Bearer $token",
                ReportListingRequestDto(
                    listingId = listingId,
                    reason = reason,
                    details = details?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
            if (response.isSuccessful) {
                MutationResult.Success(Unit)
            } else {
                val fallback = when (response.code()) {
                    409 -> "You have already submitted an open report for this listing."
                    429 -> "Report limit reached. Please wait before submitting another report."
                    else -> "Could not submit report."
                }
                MutationResult.Error(NetworkClient.parseErrorMessage(response, fallback))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout("Report submission timed out. Please avoid repeating immediately.")
        } catch (_: IOException) {
            MutationResult.Error("Network error.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Failed to report listing.")
        }
    }

    suspend fun submitTestimonial(name: String, town: String, message: String, rating: Int): MutationResult<Unit> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — connect to submit feedback.", isOffline = true)
        }
        return try {
            val response = api.submitTestimonial(
                TestimonialRequestDto(
                    name = name.trim(),
                    town = town.trim().ifEmpty { "Zambia" },
                    message = message.trim(),
                    rating = rating.coerceIn(1, 5),
                    website = ""
                )
            )
            if (response.isSuccessful) {
                MutationResult.Success(Unit)
            } else {
                MutationResult.Error(NetworkClient.parseErrorMessage(response, "Could not submit testimonial."))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout("Submission timed out.")
        } catch (_: IOException) {
            MutationResult.Error("Network error.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: "Submission failed.")
        }
    }
}
