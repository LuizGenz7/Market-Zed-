package com.example.data.repository

import com.example.data.auth.AuthRepository
import com.example.data.model.AdminOverview
import com.example.data.model.AdminPaymentRecord
import com.example.data.model.AdminPromotionRecord
import com.example.data.model.AdminReportRecord
import com.example.data.model.AdminUserRecord
import com.example.data.model.Listing
import com.example.data.model.PromotionPricing
import com.example.data.remote.AdminBroadcastRequestDto
import com.example.data.remote.AdminGrantPromotionRequestDto
import com.example.data.remote.AdminListingModerationRequestDto
import com.example.data.remote.AdminPromotionManageRequestDto
import com.example.data.remote.AdminReportResolveRequestDto
import com.example.data.remote.AdminUpdatePricingRequestDto
import com.example.data.remote.AdminUserManageRequestDto
import com.example.data.remote.ConnectivityObserver
import com.example.data.remote.MarketZedApiService
import com.example.data.remote.NetworkClient
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

sealed class AdminAccessProbeResult {
    data class Authorized(val overview: AdminOverview) : AdminAccessProbeResult()
    data object ForbiddenOrUnavailable : AdminAccessProbeResult()
}

class AdminRepository(
    private val api: MarketZedApiService,
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver
) {

    suspend fun probeAdminOverview(): AdminAccessProbeResult {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return AdminAccessProbeResult.ForbiddenOrUnavailable
        }
        val token = authRepository.getIdToken(false) ?: return AdminAccessProbeResult.ForbiddenOrUnavailable
        return try {
            var response = api.getAdminOverview("Bearer $token")
            if (response.code() == 401) {
                val refreshed = authRepository.getIdToken(true)
                if (!refreshed.isNullOrBlank()) {
                    response = api.getAdminOverview("Bearer $refreshed")
                }
            }
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AdminAccessProbeResult.Authorized(body)
            } else {
                AdminAccessProbeResult.ForbiddenOrUnavailable
            }
        } catch (_: Throwable) {
            AdminAccessProbeResult.ForbiddenOrUnavailable
        }
    }

    private suspend fun <T> executeAdminGet(
        endpointHint: String,
        fallbackError: String,
        call: suspend (bearer: String) -> Response<T>
    ): Result<T> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return Result.failure(IOException("You are offline. Connect to load admin data."))
        }
        val token = authRepository.getIdToken(false)
            ?: return Result.failure(IllegalStateException("Admin sign-in is required."))
        return try {
            var response = call("Bearer $token")
            if (response.code() == 401) {
                val refreshed = authRepository.getIdToken(true)
                if (!refreshed.isNullOrBlank()) {
                    response = call("Bearer $refreshed")
                }
            }
            when {
                response.isSuccessful && response.body() != null -> Result.success(response.body()!!)
                response.code() == 401 || response.code() == 403 ->
                    Result.failure(SecurityException("Access denied (HTTP ${response.code()}): administrator privileges required on server."))
                response.code() == 404 || response.code() == 405 || response.code() == 501 ->
                    Result.failure(IllegalStateException("Admin endpoint $endpointHint returned HTTP ${response.code()}. Ensure this route is enabled on the backend."))
                else ->
                    Result.failure(Exception(NetworkClient.parseErrorMessage(response, fallbackError)))
            }
        } catch (_: SocketTimeoutException) {
            Result.failure(IOException("Request timed out connecting to $endpointHint."))
        } catch (_: IOException) {
            Result.failure(IOException("Network error while reaching $endpointHint."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> executeAdminMutation(
        endpointHint: String,
        fallbackError: String,
        call: suspend (bearer: String) -> Response<T>
    ): MutationResult<T> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return MutationResult.Error("Offline — admin changes require an internet connection.", isOffline = true)
        }
        val token = authRepository.getIdToken(false)
            ?: return MutationResult.Error("Please sign in with an administrator account.")
        return try {
            var response = call("Bearer $token")
            if (response.code() == 401) {
                val refreshed = authRepository.getIdToken(true)
                if (!refreshed.isNullOrBlank()) {
                    response = call("Bearer $refreshed")
                }
            }
            when {
                response.isSuccessful && response.body() != null -> MutationResult.Success(response.body()!!)
                response.code() == 401 || response.code() == 403 ->
                    MutationResult.Error("Access denied (HTTP ${response.code()}): administrator privileges required.")
                response.code() == 404 || response.code() == 405 || response.code() == 501 ->
                    MutationResult.Error("Admin endpoint $endpointHint returned HTTP ${response.code()}. Ensure this route is enabled on the backend.")
                else ->
                    MutationResult.Error(NetworkClient.parseErrorMessage(response, fallbackError))
            }
        } catch (_: SocketTimeoutException) {
            MutationResult.AmbiguousTimeout("Admin action timed out. Refresh to verify whether the change took effect.")
        } catch (_: IOException) {
            MutationResult.Error("Network error communicating with $endpointHint.", isOffline = true)
        } catch (e: Exception) {
            MutationResult.Error(e.localizedMessage ?: fallbackError)
        }
    }

    suspend fun getOverview(): Result<AdminOverview> =
        executeAdminGet("GET /api/admin/overview", "Could not load admin overview.") {
            api.getAdminOverview(it)
        }

    suspend fun getListings(status: String? = null, query: String? = null): Result<List<Listing>> =
        executeAdminGet("GET /api/admin/listings", "Could not load listings moderation queue.") {
            api.getAdminListings(
                bearerToken = it,
                status = status?.takeIf { s -> s.isNotBlank() && s != "all" },
                query = query?.trim()?.takeIf { q -> q.isNotBlank() }
            )
        }.map { it.items }

    suspend fun moderateListing(
        listingId: String,
        action: String,
        reason: String? = null
    ): MutationResult<Map<String, Any?>> =
        executeAdminMutation("PATCH /api/admin/listings", "Could not update listing moderation.") {
            api.moderateAdminListing(
                bearerToken = it,
                body = AdminListingModerationRequestDto(
                    listingId = listingId,
                    action = action,
                    reason = reason?.trim()?.takeIf { r -> r.isNotEmpty() }
                )
            )
        }

    suspend fun getPromotions(status: String? = null): Result<List<AdminPromotionRecord>> =
        executeAdminGet("GET /api/admin/promotions", "Could not load promotions.") {
            api.getAdminPromotions(
                bearerToken = it,
                status = status?.takeIf { s -> s.isNotBlank() && s != "all" }
            )
        }.map { it.items }

    suspend fun getPromotionPricing(): Result<PromotionPricing> {
        if (!connectivityObserver.checkCurrentlyConnected()) {
            return Result.failure(IOException("Offline — cannot load live promotion pricing."))
        }
        return try {
            val response = api.getPromotionPricing()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(NetworkClient.parseErrorMessage(response, "Could not load promotion pricing.")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun managePromotion(
        promotionId: String,
        action: String,
        durationDays: Int? = null,
        reason: String? = null,
        campaignFormat: String? = null
    ): MutationResult<Map<String, Any?>> =
        executeAdminMutation("PATCH /api/admin/promotions", "Could not update promotion.") {
            api.manageAdminPromotion(
                bearerToken = it,
                body = AdminPromotionManageRequestDto(
                    promotionId = promotionId,
                    action = action,
                    durationDays = durationDays,
                    reason = reason?.trim()?.takeIf { r -> r.isNotEmpty() },
                    campaignFormat = campaignFormat?.trim()?.takeIf { f -> f.isNotEmpty() }
                )
            )
        }

    suspend fun grantPromotion(
        listingId: String,
        durationDays: Int,
        note: String? = null,
        campaignFormat: String? = null
    ): MutationResult<Map<String, Any?>> =
        executeAdminMutation("POST /api/admin/promotions", "Could not grant promotion.") {
            api.grantAdminPromotion(
                bearerToken = it,
                body = AdminGrantPromotionRequestDto(
                    listingId = listingId.trim(),
                    durationDays = durationDays,
                    note = note?.trim()?.takeIf { n -> n.isNotEmpty() },
                    campaignFormat = campaignFormat?.trim()?.takeIf { f -> f.isNotEmpty() }
                )
            )
        }

    suspend fun updatePromotionPricing(prices: Map<String, Double>): MutationResult<PromotionPricing> =
        executeAdminMutation("PUT /api/admin/promotions/pricing", "Could not update promotion pricing.") {
            api.updateAdminPromotionPricing(
                bearerToken = it,
                body = AdminUpdatePricingRequestDto(prices = prices)
            )
        }

    suspend fun getReports(status: String? = null): Result<List<AdminReportRecord>> =
        executeAdminGet("GET /api/admin/reports", "Could not load reports.") {
            api.getAdminReports(
                bearerToken = it,
                status = status?.takeIf { s -> s.isNotBlank() && s != "all" }
            )
        }.map { it.items }

    suspend fun resolveReport(
        reportId: String,
        action: String,
        resolutionNote: String? = null
    ): MutationResult<Map<String, Any?>> =
        executeAdminMutation("PATCH /api/admin/reports", "Could not resolve report.") {
            api.resolveAdminReport(
                bearerToken = it,
                body = AdminReportResolveRequestDto(
                    reportId = reportId,
                    action = action,
                    resolutionNote = resolutionNote?.trim()?.takeIf { r -> r.isNotEmpty() }
                )
            )
        }

    suspend fun getUsers(query: String? = null, filter: String? = null): Result<List<AdminUserRecord>> =
        executeAdminGet("GET /api/admin/users", "Could not load users.") {
            api.getAdminUsers(
                bearerToken = it,
                query = query?.trim()?.takeIf { q -> q.isNotBlank() },
                filter = filter?.takeIf { f -> f.isNotBlank() && f != "all" }
            )
        }.map { it.items }

    suspend fun manageUser(
        uid: String,
        action: String,
        reason: String? = null
    ): MutationResult<Map<String, Any?>> =
        executeAdminMutation("PATCH /api/admin/users", "Could not update user account.") {
            api.manageAdminUser(
                bearerToken = it,
                body = AdminUserManageRequestDto(
                    uid = uid,
                    action = action,
                    reason = reason?.trim()?.takeIf { r -> r.isNotEmpty() }
                )
            )
        }

    suspend fun getPayments(status: String? = null): Result<List<AdminPaymentRecord>> =
        executeAdminGet("GET /api/admin/payments", "Could not load platform payments.") {
            api.getAdminPayments(
                bearerToken = it,
                status = status?.takeIf { s -> s.isNotBlank() && s != "all" }
            )
        }.map { it.items }

    suspend fun sendBroadcast(
        title: String,
        message: String,
        categoryId: String? = null,
        listingId: String? = null
    ): MutationResult<Map<String, Any?>> =
        executeAdminMutation("POST /api/admin/broadcast", "Could not send broadcast announcement.") {
            api.sendAdminBroadcast(
                bearerToken = it,
                body = AdminBroadcastRequestDto(
                    title = title.trim(),
                    message = message.trim(),
                    categoryId = categoryId?.trim()?.takeIf { c -> c.isNotEmpty() },
                    listingId = listingId?.trim()?.takeIf { l -> l.isNotEmpty() }
                )
            )
        }
}
