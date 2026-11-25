package com.example.data.remote

import com.example.data.model.AdminOverview
import com.example.data.model.AdminPaymentRecord
import com.example.data.model.AdminPromotionRecord
import com.example.data.model.AdminReportRecord
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppNotification
import com.example.data.model.CategoryDetails
import com.example.data.model.ContactPreferences
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.model.ListingLocation
import com.example.data.model.PaymentRecord
import com.example.data.model.PromotionPricing
import com.example.data.model.PromotionRecord
import com.example.data.model.Testimonial
import com.example.data.model.UserProfile
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SearchResponseDto(
    val items: List<Listing> = emptyList(),
    val nextCursor: String? = null
)

@JsonClass(generateAdapter = true)
data class ItemsWrapperDto<T>(
    val items: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    val error: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequestDto(
    val firstName: String,
    val lastName: String,
    val contactNumber: String,
    val dateOfBirth: String? = null,
    val whatsappNumber: String? = null,
    val alternativeContact: String? = null,
    val accountType: String = "individual",
    val province: String? = null,
    val town: String? = null
)

@JsonClass(generateAdapter = true)
data class CloudinaryUploadResponseDto(
    val url: String = "",
    val thumbUrl: String? = null,
    val publicId: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val format: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmitListingRequestDto(
    val categoryId: String,
    val subcategory: String,
    val title: String,
    val description: String,
    val price: String? = null,
    val location: ListingLocation,
    val images: List<ListingImage>,
    val contactPreferences: ContactPreferences,
    val categoryDetails: CategoryDetails,
    val ownershipDeclarationAccepted: Boolean,
    val ownershipDeclarationVersion: String
)

@JsonClass(generateAdapter = true)
data class SubmitListingResponseDto(
    val listingId: String? = null,
    val status: String? = null,
    val public: Boolean? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ManageListingRequestDto(
    val listingId: String,
    val action: String
)

@JsonClass(generateAdapter = true)
data class ReviseListingRequestDto(
    val listingId: String,
    val changes: Map<String, @JvmSuppressWildcards Any?>
)

@JsonClass(generateAdapter = true)
data class CreatePaymentRequestDto(
    val purpose: String,
    val accountNumber: String,
    val payerType: String,
    val listingId: String? = null,
    val promotionId: String? = null,
    val durationDays: Int? = null,
    val campaignFormat: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentStatusRequestDto(
    val paymentId: String
)

@JsonClass(generateAdapter = true)
data class PaymentResponseDto(
    val paymentId: String? = null,
    val referenceId: String? = null,
    val status: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MarkNotificationReadRequestDto(
    val notificationId: String
)

@JsonClass(generateAdapter = true)
data class ReportListingRequestDto(
    val listingId: String,
    val reason: String,
    val details: String? = null
)

@JsonClass(generateAdapter = true)
data class TestimonialRequestDto(
    val name: String,
    val town: String,
    val message: String,
    val rating: Int,
    val website: String = ""
)

@JsonClass(generateAdapter = true)
data class AdminListingModerationRequestDto(
    val listingId: String,
    val action: String,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminPromotionManageRequestDto(
    val promotionId: String,
    val action: String,
    val durationDays: Int? = null,
    val reason: String? = null,
    val campaignFormat: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminGrantPromotionRequestDto(
    val listingId: String,
    val durationDays: Int,
    val note: String? = null,
    val campaignFormat: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminUpdatePricingRequestDto(
    val prices: Map<String, Double>
)

@JsonClass(generateAdapter = true)
data class AdminReportResolveRequestDto(
    val reportId: String,
    val action: String,
    val resolutionNote: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminUserManageRequestDto(
    val uid: String,
    val action: String,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminBroadcastRequestDto(
    val title: String,
    val message: String,
    val categoryId: String? = null,
    val listingId: String? = null
)

interface MarketZedApiService {

    @GET("api/search")
    suspend fun searchListings(
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
        @Query("subcategory") subcategory: String? = null,
        @Query("province") province: String? = null,
        @Query("town") town: String? = null,
        @Query("condition") condition: String? = null,
        @Query("sort") sort: String? = "newest",
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<SearchResponseDto>

    @GET("api/promotions/pricing")
    suspend fun getPromotionPricing(): Response<PromotionPricing>

    @POST("api/testimonials")
    suspend fun submitTestimonial(
        @Body body: TestimonialRequestDto
    ): Response<Map<String, Any?>>

    @GET("api/account/profile")
    suspend fun getProfile(
        @Header("Authorization") bearerToken: String
    ): Response<UserProfile>

    @PUT("api/account/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearerToken: String,
        @Body body: ProfileUpdateRequestDto
    ): Response<UserProfile>

    @GET("api/account/notifications")
    suspend fun getNotifications(
        @Header("Authorization") bearerToken: String
    ): Response<ItemsWrapperDto<AppNotification>>

    @PATCH("api/account/notifications")
    suspend fun markNotificationRead(
        @Header("Authorization") bearerToken: String,
        @Body body: MarkNotificationReadRequestDto
    ): Response<Map<String, Any?>>

    @Multipart
    @POST("api/cloudinary/upload")
    suspend fun uploadImage(
        @Header("Authorization") bearerToken: String,
        @Part file: MultipartBody.Part
    ): Response<CloudinaryUploadResponseDto>

    @POST("api/listings/submit")
    suspend fun submitListing(
        @Header("Authorization") bearerToken: String,
        @Body body: SubmitListingRequestDto
    ): Response<SubmitListingResponseDto>

    @GET("api/listings/mine")
    suspend fun getMyListings(
        @Header("Authorization") bearerToken: String
    ): Response<ItemsWrapperDto<Listing>>

    @PATCH("api/listings/manage")
    suspend fun manageListing(
        @Header("Authorization") bearerToken: String,
        @Body body: ManageListingRequestDto
    ): Response<Map<String, Any?>>

    @POST("api/listings/revise")
    suspend fun reviseListing(
        @Header("Authorization") bearerToken: String,
        @Body body: ReviseListingRequestDto
    ): Response<Map<String, Any?>>

    @POST("api/payments/create")
    suspend fun createPayment(
        @Header("Authorization") bearerToken: String,
        @Body body: CreatePaymentRequestDto
    ): Response<PaymentResponseDto>

    @POST("api/payments/status")
    suspend fun checkPaymentStatus(
        @Header("Authorization") bearerToken: String,
        @Body body: PaymentStatusRequestDto
    ): Response<PaymentResponseDto>

    @GET("api/payments/mine")
    suspend fun getMyPayments(
        @Header("Authorization") bearerToken: String
    ): Response<ItemsWrapperDto<PaymentRecord>>

    @GET("api/promotions/mine")
    suspend fun getMyPromotions(
        @Header("Authorization") bearerToken: String
    ): Response<ItemsWrapperDto<PromotionRecord>>

    @POST("api/reports")
    suspend fun reportListing(
        @Header("Authorization") bearerToken: String,
        @Body body: ReportListingRequestDto
    ): Response<Map<String, Any?>>

    // --- Admin Endpoints (server-verified admin token required) ---

    @GET("api/admin/overview")
    suspend fun getAdminOverview(
        @Header("Authorization") bearerToken: String
    ): Response<AdminOverview>

    @GET("api/admin/listings")
    suspend fun getAdminListings(
        @Header("Authorization") bearerToken: String,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ItemsWrapperDto<Listing>>

    @PATCH("api/admin/listings")
    suspend fun moderateAdminListing(
        @Header("Authorization") bearerToken: String,
        @Body body: AdminListingModerationRequestDto
    ): Response<Map<String, Any?>>

    @GET("api/admin/promotions")
    suspend fun getAdminPromotions(
        @Header("Authorization") bearerToken: String,
        @Query("status") status: String? = null
    ): Response<ItemsWrapperDto<AdminPromotionRecord>>

    @PATCH("api/admin/promotions")
    suspend fun manageAdminPromotion(
        @Header("Authorization") bearerToken: String,
        @Body body: AdminPromotionManageRequestDto
    ): Response<Map<String, Any?>>

    @POST("api/admin/promotions")
    suspend fun grantAdminPromotion(
        @Header("Authorization") bearerToken: String,
        @Body body: AdminGrantPromotionRequestDto
    ): Response<Map<String, Any?>>

    @PUT("api/admin/promotions/pricing")
    suspend fun updateAdminPromotionPricing(
        @Header("Authorization") bearerToken: String,
        @Body body: AdminUpdatePricingRequestDto
    ): Response<PromotionPricing>

    @GET("api/admin/reports")
    suspend fun getAdminReports(
        @Header("Authorization") bearerToken: String,
        @Query("status") status: String? = null
    ): Response<ItemsWrapperDto<AdminReportRecord>>

    @PATCH("api/admin/reports")
    suspend fun resolveAdminReport(
        @Header("Authorization") bearerToken: String,
        @Body body: AdminReportResolveRequestDto
    ): Response<Map<String, Any?>>

    @GET("api/admin/users")
    suspend fun getAdminUsers(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String? = null,
        @Query("filter") filter: String? = null
    ): Response<ItemsWrapperDto<AdminUserRecord>>

    @PATCH("api/admin/users")
    suspend fun manageAdminUser(
        @Header("Authorization") bearerToken: String,
        @Body body: AdminUserManageRequestDto
    ): Response<Map<String, Any?>>

    @GET("api/admin/payments")
    suspend fun getAdminPayments(
        @Header("Authorization") bearerToken: String,
        @Query("status") status: String? = null
    ): Response<ItemsWrapperDto<AdminPaymentRecord>>

    @POST("api/admin/broadcast")
    suspend fun sendAdminBroadcast(
        @Header("Authorization") bearerToken: String,
        @Body body: AdminBroadcastRequestDto
    ): Response<Map<String, Any?>>
}
