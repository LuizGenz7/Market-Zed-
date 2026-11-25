package com.example.ui.screens.account

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.PhoneValidator
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthUserState
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.dev.DevFixturesManager
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.model.PaymentRecord
import com.example.data.model.PromotionPricing
import com.example.data.model.PromotionRecord
import com.example.data.model.UserProfile
import com.example.data.notifications.LocalAnnouncement
import com.example.data.notifications.NotificationPreferences
import com.example.data.notifications.NotificationPreferencesRepository
import com.example.data.remote.ProfileUpdateRequestDto
import com.example.data.repository.AccountErrorKind
import com.example.data.repository.AccountFetchException
import com.example.data.repository.AccountRepository
import com.example.data.repository.AdminAccessProbeResult
import com.example.data.repository.AdminRepository
import com.example.data.repository.InMemoryLocalAccountCache
import com.example.data.repository.LocalAccountCacheStore
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MutationResult
import com.example.data.sync.BackgroundSyncStatus
import com.example.data.sync.MarketplaceSyncScheduler
import com.example.ui.components.PaymentUiStatePhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class AccountUiState(
    val auth: AuthUserState = AuthUserState(),
    val authBusy: Boolean = false,
    val authMessage: String? = null,
    val authError: String? = null,
    val profile: UserProfile? = null,
    val profileLoading: Boolean = false,
    val profileError: String? = null,
    val profileSaveMessage: String? = null,
    val myListings: List<Listing> = emptyList(),
    val myListingsLoading: Boolean = false,
    val myListingsRefreshing: Boolean = false,
    val myListingsOfflineNotice: String? = null,
    val myListingsError: String? = null,
    val myListingsActionMessage: String? = null,
    val sessionExpiredPrompt: String? = null,
    val notifications: List<AppNotification> = emptyList(),
    val notificationsLoading: Boolean = false,
    val notificationsError: String? = null,
    val payments: List<PaymentRecord> = emptyList(),
    val paymentsLoading: Boolean = false,
    val paymentsError: String? = null,
    val promotions: List<PromotionRecord> = emptyList(),
    val promotionPricing: PromotionPricing? = null,
    val promotionsLoading: Boolean = false,
    val promotionsError: String? = null,
    val activePaymentId: String? = null,
    val activePaymentReference: String? = null,
    val activePaymentStatus: String? = null,
    val activePaymentMessage: String? = null,
    val activePaymentPurpose: String? = null,
    val activePaymentAmountZmw: Double? = null,
    val paymentBusy: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val cacheClearedMessage: String? = null,
    val backgroundSyncStatus: BackgroundSyncStatus = BackgroundSyncStatus(),
    val notificationPrefs: NotificationPreferences = NotificationPreferences(),
    val localAnnouncements: List<LocalAnnouncement> = emptyList(),
    val adminBackendVerified: Boolean = false,
    val devFixturesAvailable: Boolean = false,
    val devFixturesSeeded: Boolean = false,
    val devFixturesCount: Int = 0
) {
    val unreadNotificationCount: Int
        get() = notifications.count { !it.read } + localAnnouncements.count { !it.read }

    val isAdmin: Boolean
        get() = auth.isSignedIn && (auth.isAdminClaim || profile?.hasAdminRole == true || adminBackendVerified)

    val verificationFeeZmw: Double
        get() = profile?.verificationFeeZmw
            ?: promotionPricing?.verificationFeeOrDefault(MarketZedCatalogue.DEFAULT_VERIFICATION_FEE_ZMW)
            ?: MarketZedCatalogue.DEFAULT_VERIFICATION_FEE_ZMW

    fun resolvePaymentPhase(
        purposeLabel: String,
        fallbackAmountZmw: Double,
        successDetail: String
    ): PaymentUiStatePhase {
        val status = activePaymentStatus?.trim()?.lowercase()
        val id = activePaymentId
        val amt = activePaymentAmountZmw ?: fallbackAmountZmw
        return when {
            paymentBusy -> PaymentUiStatePhase.Processing(
                message = activePaymentMessage ?: "Please wait while we confirm your payment."
            )
            status == "successful" -> PaymentUiStatePhase.Successful(
                paymentId = id,
                amountZmw = amt,
                purposeTitle = activePaymentPurpose ?: purposeLabel,
                headline = "Payment confirmed.",
                detail = activePaymentMessage ?: successDetail
            )
            status == "cancelled" -> PaymentUiStatePhase.Cancelled(
                paymentId = id,
                message = activePaymentMessage ?: "Payment was cancelled."
            )
            status == "failed" || status == "expired" -> PaymentUiStatePhase.Failed(
                paymentId = id,
                message = activePaymentMessage ?: "We could not confirm your payment.\nPlease try again or contact support."
            )
            !id.isNullOrBlank() -> PaymentUiStatePhase.Pending(
                paymentId = id,
                amountZmw = amt,
                purposeTitle = activePaymentPurpose ?: purposeLabel,
                message = activePaymentMessage ?: "Your payment is being processed."
            )
            !activePaymentMessage.isNullOrBlank() -> PaymentUiStatePhase.Failed(
                paymentId = null,
                message = activePaymentMessage
            )
            else -> PaymentUiStatePhase.Idle
        }
    }
}

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val localUserRepository: LocalUserRepository,
    private val marketplaceRepository: MarketplaceRepository,
    private val syncScheduler: MarketplaceSyncScheduler,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val localAccountCache: LocalAccountCacheStore = InMemoryLocalAccountCache(),
    private val adminRepository: AdminRepository? = null,
    private val devFixturesManager: DevFixturesManager? = null
) : ViewModel() {

    private val _authBusy = MutableStateFlow(false)
    private val _authMessage = MutableStateFlow<String?>(null)
    private val _authError = MutableStateFlow<String?>(null)

    private val _profile = MutableStateFlow<UserProfile?>(null)
    private val _profileLoading = MutableStateFlow(false)
    private val _profileError = MutableStateFlow<String?>(null)
    private val _profileSaveMessage = MutableStateFlow<String?>(null)

    private val _myListings = MutableStateFlow<List<Listing>>(emptyList())
    private val _myListingsLoading = MutableStateFlow(false)
    private val _myListingsRefreshing = MutableStateFlow(false)
    private val _myListingsOfflineNotice = MutableStateFlow<String?>(null)
    private val _myListingsError = MutableStateFlow<String?>(null)
    private val _myListingsActionMessage = MutableStateFlow<String?>(null)
    private val _sessionExpiredPrompt = MutableStateFlow<String?>(null)

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val _notificationsLoading = MutableStateFlow(false)
    private val _notificationsError = MutableStateFlow<String?>(null)

    private val _payments = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private val _paymentsLoading = MutableStateFlow(false)
    private val _paymentsError = MutableStateFlow<String?>(null)

    private val _promotions = MutableStateFlow<List<PromotionRecord>>(emptyList())
    private val _promotionPricing = MutableStateFlow<PromotionPricing?>(null)
    private val _promotionsLoading = MutableStateFlow(false)
    private val _promotionsError = MutableStateFlow<String?>(null)

    private val _activePaymentId = MutableStateFlow<String?>(null)
    private val _activePaymentReference = MutableStateFlow<String?>(null)
    private val _activePaymentStatus = MutableStateFlow<String?>(null)
    private val _activePaymentMessage = MutableStateFlow<String?>(null)
    private val _activePaymentPurpose = MutableStateFlow<String?>(null)
    private val _activePaymentAmountZmw = MutableStateFlow<Double?>(null)
    private val _paymentBusy = MutableStateFlow(false)

    private val _cacheClearedMessage = MutableStateFlow<String?>(null)
    private val _adminBackendVerified = MutableStateFlow(false)
    private val _devFixturesSeededFlow = devFixturesManager?.isSeeded ?: MutableStateFlow(false)

    val uiState: StateFlow<AccountUiState> = combine(
        authRepository.authState,
        _authBusy,
        _authMessage,
        _authError,
        _profile,
        _profileLoading,
        _profileError,
        _profileSaveMessage,
        _myListings,
        _myListingsLoading,
        _myListingsRefreshing,
        _myListingsOfflineNotice,
        _myListingsError,
        _myListingsActionMessage,
        _sessionExpiredPrompt,
        combine(
            _notifications,
            notificationPreferencesRepository.deletedNotificationIds
        ) { list, deleted ->
            list.distinctBy { it.id }.filterNot { it.id in deleted }
        },
        _notificationsLoading,
        _notificationsError,
        _payments,
        _paymentsLoading,
        _paymentsError,
        _promotions,
        _promotionPricing,
        _promotionsLoading,
        _promotionsError,
        _activePaymentId,
        _activePaymentReference,
        _activePaymentStatus,
        _activePaymentMessage,
        _activePaymentPurpose,
        _activePaymentAmountZmw,
        _paymentBusy,
        localUserRepository.observeSettings(),
        _cacheClearedMessage,
        syncScheduler.observeSyncStatus(marketplaceRepository, localUserRepository),
        notificationPreferencesRepository.preferences,
        notificationPreferencesRepository.announcements,
        _adminBackendVerified,
        _devFixturesSeededFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        AccountUiState(
            auth = arr[0] as AuthUserState,
            authBusy = arr[1] as Boolean,
            authMessage = arr[2] as String?,
            authError = arr[3] as String?,
            profile = arr[4] as UserProfile?,
            profileLoading = arr[5] as Boolean,
            profileError = arr[6] as String?,
            profileSaveMessage = arr[7] as String?,
            myListings = arr[8] as List<Listing>,
            myListingsLoading = arr[9] as Boolean,
            myListingsRefreshing = arr[10] as Boolean,
            myListingsOfflineNotice = arr[11] as String?,
            myListingsError = arr[12] as String?,
            myListingsActionMessage = arr[13] as String?,
            sessionExpiredPrompt = arr[14] as String?,
            notifications = arr[15] as List<AppNotification>,
            notificationsLoading = arr[16] as Boolean,
            notificationsError = arr[17] as String?,
            payments = arr[18] as List<PaymentRecord>,
            paymentsLoading = arr[19] as Boolean,
            paymentsError = arr[20] as String?,
            promotions = arr[21] as List<PromotionRecord>,
            promotionPricing = arr[22] as PromotionPricing?,
            promotionsLoading = arr[23] as Boolean,
            promotionsError = arr[24] as String?,
            activePaymentId = arr[25] as String?,
            activePaymentReference = arr[26] as String?,
            activePaymentStatus = arr[27] as String?,
            activePaymentMessage = arr[28] as String?,
            activePaymentPurpose = arr[29] as String?,
            activePaymentAmountZmw = arr[30] as Double?,
            paymentBusy = arr[31] as Boolean,
            settings = arr[32] as AppSettings,
            cacheClearedMessage = arr[33] as String?,
            backgroundSyncStatus = arr[34] as BackgroundSyncStatus,
            notificationPrefs = arr[35] as NotificationPreferences,
            localAnnouncements = arr[36] as List<LocalAnnouncement>,
            adminBackendVerified = arr[37] as Boolean,
            devFixturesAvailable = devFixturesManager?.isFeatureAvailable == true,
            devFixturesSeeded = arr[38] as Boolean,
            devFixturesCount = devFixturesManager?.getFixtureCount() ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AccountUiState()
    )

    init {
        viewModelScope.launch {
            _promotionPricing.value = localAccountCache.getPromotionPricing()
                ?: marketplaceRepository.getCachedPromotionPricing()
        }
        viewModelScope.launch {
            authRepository.authState.collect { auth ->
                if (auth.isSignedIn) {
                    hydrateLocalAccountCache(auth.userScope)
                    loadProfile()
                    verifyAdminBackendAccess()
                    loadNotifications()
                    loadMyListings()
                    loadPayments()
                    loadPromotionsAndPricing()
                } else {
                    clearPrivateMemoryState()
                }
            }
        }
    }

    fun verifyAdminBackendAccess() {
        val repo = adminRepository ?: return
        if (!authRepository.authState.value.isSignedIn) {
            _adminBackendVerified.value = false
            return
        }
        viewModelScope.launch {
            _adminBackendVerified.value = when (repo.probeAdminOverview()) {
                is AdminAccessProbeResult.Authorized -> true
                AdminAccessProbeResult.ForbiddenOrUnavailable -> false
            }
        }
    }

    private suspend fun hydrateLocalAccountCache(userScope: String) {
        localAccountCache.getProfile(userScope)?.let { _profile.value = it }
        val cachedListings = localAccountCache.getMyListings(userScope)
        if (cachedListings.isNotEmpty()) _myListings.value = cachedListings
        val cachedNotifs = localAccountCache.getNotifications(userScope)
        if (cachedNotifs.isNotEmpty()) _notifications.value = cachedNotifs
        val cachedPayments = localAccountCache.getPayments(userScope)
        if (cachedPayments.isNotEmpty()) _payments.value = cachedPayments
        val cachedPromos = localAccountCache.getPromotions(userScope)
        if (cachedPromos.isNotEmpty()) _promotions.value = cachedPromos
        if (_promotionPricing.value == null) {
            _promotionPricing.value = localAccountCache.getPromotionPricing()
                ?: marketplaceRepository.getCachedPromotionPricing()
        }
    }

    private fun clearPrivateMemoryState() {
        _profile.value = null
        _profileError.value = null
        _myListings.value = emptyList()
        _myListingsLoading.value = false
        _myListingsRefreshing.value = false
        _myListingsOfflineNotice.value = null
        _myListingsError.value = null
        _notifications.value = emptyList()
        _notificationsError.value = null
        _payments.value = emptyList()
        _paymentsError.value = null
        _promotions.value = emptyList()
        _promotionsError.value = null
        _activePaymentId.value = null
        _activePaymentReference.value = null
        _activePaymentStatus.value = null
        _activePaymentMessage.value = null
        _activePaymentPurpose.value = null
        _activePaymentAmountZmw.value = null
        _adminBackendVerified.value = false
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Enter both email and password."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            _authMessage.value = null
            authRepository.signInWithEmail(email, password)
                .onSuccess { onSuccess() }
                .onFailure { _authError.value = it.localizedMessage }
            _authBusy.value = false
        }
    }

    fun register(fullName: String, email: String, password: String, confirm: String, onSuccess: () -> Unit = {}) {
        if (fullName.trim().length < 2) {
            _authError.value = "Enter your full name."
            return
        }
        if (!PhoneValidator.isValidEmail(email)) {
            _authError.value = "Enter a valid email address."
            return
        }
        if (password.length < 8) {
            _authError.value = "Password must be at least 8 characters."
            return
        }
        if (password != confirm) {
            _authError.value = "Passwords do not match."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            _authMessage.value = null
            authRepository.registerWithEmail(fullName, email, password)
                .onSuccess {
                    _authMessage.value =
                        "Account created! A verification link was sent to your email. Note: K30 account activation is still required to post in normal categories."
                    onSuccess()
                }
                .onFailure { _authError.value = it.localizedMessage }
            _authBusy.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        if (!PhoneValidator.isValidEmail(email)) {
            _authError.value = "Enter your account email address first."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            authRepository.sendPasswordReset(email)
                .onSuccess {
                    _authMessage.value = "If an account exists for $email, a password reset email has been sent."
                }
                .onFailure { _authError.value = it.localizedMessage }
            _authBusy.value = false
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _authBusy.value = true
            authRepository.sendEmailVerification()
                .onSuccess { _authMessage.value = "Verification email sent. Check your inbox." }
                .onFailure { _authError.value = it.localizedMessage }
            _authBusy.value = false
        }
    }

    fun refreshEmailVerificationStatus() {
        viewModelScope.launch {
            authRepository.refreshUser()
        }
    }

    fun clearSessionExpiredPrompt() {
        _sessionExpiredPrompt.value = null
    }

    fun signOut() {
        val scope = authRepository.authState.value.userScope
        localAccountCache.clearUserScope(scope)
        authRepository.signOut()
        clearPrivateMemoryState()
    }

    fun clearAuthBanners() {
        _authError.value = null
        _authMessage.value = null
    }

    fun loadProfile() {
        val auth = authRepository.authState.value
        if (!auth.isSignedIn) return
        viewModelScope.launch {
            if (_profile.value == null) {
                _profileLoading.value = true
            }
            _profileError.value = null
            accountRepository.getProfile()
                .onSuccess { profile ->
                    _profile.value = profile
                    localAccountCache.saveProfile(auth.userScope, profile)
                }
                .onFailure {
                    if (_profile.value == null) {
                        _profileError.value = it.localizedMessage
                    }
                }
            _profileLoading.value = false
        }
    }

    fun saveProfile(
        firstName: String,
        lastName: String,
        contactNumber: String,
        dateOfBirth: String,
        whatsappNumber: String,
        alternativeContact: String,
        accountType: String,
        province: String,
        town: String
    ) {
        if (firstName.trim().isEmpty() || lastName.trim().isEmpty() || contactNumber.trim().isEmpty()) {
            _profileError.value = "First name, last name, and contact number are required."
            return
        }
        val normalizedContact = PhoneValidator.normalizeZambianNumber(contactNumber)
        if (normalizedContact == null) {
            _profileError.value = "Enter a valid Zambian contact number (e.g. 969333123, 0969331234, or 260969331234)."
            return
        }
        val trimmedWa = whatsappNumber.trim()
        val normalizedWhatsapp = if (trimmedWa.isNotEmpty()) {
            PhoneValidator.normalizeZambianNumber(trimmedWa) ?: run {
                _profileError.value = "Enter a valid Zambian WhatsApp number (e.g. 969333123, 0969331234, or 260969331234)."
                return
            }
        } else {
            null
        }
        val normalizedAlt = alternativeContact.trim().takeIf { it.isNotEmpty() }?.let { alt ->
            PhoneValidator.normalizeZambianNumber(alt) ?: alt
        }
        viewModelScope.launch {
            _profileLoading.value = true
            _profileError.value = null
            _profileSaveMessage.value = null
            val dto = ProfileUpdateRequestDto(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                contactNumber = normalizedContact,
                dateOfBirth = dateOfBirth.trim().takeIf { it.isNotEmpty() },
                whatsappNumber = normalizedWhatsapp,
                alternativeContact = normalizedAlt,
                accountType = if (accountType == "business") "business" else "individual",
                province = province.trim().takeIf { it.isNotEmpty() },
                town = town.trim().takeIf { it.isNotEmpty() }
            )
            when (val res = accountRepository.updateProfile(dto)) {
                is MutationResult.Success -> {
                    _profile.value = res.data
                    localAccountCache.saveProfile(authRepository.authState.value.userScope, res.data)
                    _profileSaveMessage.value = "Profile updated."
                }
                is MutationResult.Error -> _profileError.value = res.message
                is MutationResult.AmbiguousTimeout -> _profileError.value = res.message
            }
            _profileLoading.value = false
        }
    }

    fun clearProfileMessages() {
        _profileSaveMessage.value = null
        _profileError.value = null
    }

    fun loadMyListings() {
        val auth = authRepository.authState.value
        if (!auth.isSignedIn) return
        viewModelScope.launch {
            if (_myListings.value.isEmpty()) {
                _myListingsLoading.value = true
            } else {
                _myListingsRefreshing.value = true
            }
            _myListingsError.value = null
            _myListingsOfflineNotice.value = null
            accountRepository.getMyListings()
                .onSuccess { items ->
                    _myListings.value = items
                    _sessionExpiredPrompt.value = null
                    _myListingsOfflineNotice.value = null
                    localAccountCache.saveMyListings(auth.userScope, items)
                }
                .onFailure { err ->
                    val kind = (err as? AccountFetchException)?.kind
                    when (kind) {
                        AccountErrorKind.SESSION_EXPIRED -> {
                            _sessionExpiredPrompt.value = err.message
                        }
                        AccountErrorKind.OFFLINE, AccountErrorKind.NETWORK -> {
                            if (_myListings.value.isNotEmpty()) {
                                _myListingsOfflineNotice.value = err.message
                            } else {
                                _myListingsError.value = err.message
                            }
                        }
                        AccountErrorKind.PERMISSION_DENIED, AccountErrorKind.SERVER, null -> {
                            val msg = err.localizedMessage ?: "Could not load your listings."
                            if (_myListings.value.isEmpty()) {
                                _myListingsError.value = msg
                            } else {
                                _myListingsActionMessage.value = msg
                            }
                        }
                    }
                }
            _myListingsLoading.value = false
            _myListingsRefreshing.value = false
        }
    }

    fun manageListing(listingId: String, action: String) {
        viewModelScope.launch {
            _myListingsActionMessage.value = null
            when (val res = accountRepository.manageListing(listingId, action)) {
                is MutationResult.Success -> {
                    _myListingsActionMessage.value = "Listing updated."
                    loadMyListings()
                    marketplaceRepository.refreshHomeFeeds(force = true)
                }
                is MutationResult.Error -> _myListingsActionMessage.value = res.message
                is MutationResult.AmbiguousTimeout -> _myListingsActionMessage.value = res.message
            }
        }
    }

    fun reviseListing(listingId: String, changes: Map<String, Any?>, onComplete: (Boolean, String) -> Unit) {
        val currentUid = authRepository.authState.value.uid
        val existing = _myListings.value.firstOrNull { it.id == listingId }
        if (existing == null) {
            onComplete(false, "Listing not found in your account.")
            return
        }
        if (!existing.ownerId.isNullOrBlank() && !currentUid.isNullOrBlank() && existing.ownerId != currentUid) {
            onComplete(false, "You can only edit your own listings.")
            return
        }
        val newTitle = (changes["title"] as? String) ?: existing.title
        val newDesc = (changes["description"] as? String) ?: existing.description
        val newSub = (changes["subcategory"] as? String) ?: existing.subcategory
        val policyErr = MarketZedCatalogue.validatePropertyAndLandPolicy(
            categoryId = existing.categoryId,
            subcategory = newSub,
            title = newTitle,
            description = newDesc,
            priceMode = existing.priceMode
        )
        if (policyErr != null) {
            onComplete(false, policyErr)
            return
        }
        // Wanted listings must never carry images.
        val sanitizedChanges = if (existing.isWanted) {
            changes.toMutableMap().apply { remove("images") }
        } else {
            changes
        }
        viewModelScope.launch {
            when (val res = accountRepository.reviseListing(listingId, sanitizedChanges)) {
                is MutationResult.Success -> {
                    _myListingsActionMessage.value = "Listing changes saved."
                    loadMyListings()
                    marketplaceRepository.refreshHomeFeeds(force = true)
                    onComplete(true, "Listing changes saved.")
                }
                is MutationResult.Error -> onComplete(false, res.message)
                is MutationResult.AmbiguousTimeout -> onComplete(false, res.message)
            }
        }
    }

    suspend fun uploadImageFromUriForEdit(uri: Uri): MutationResult<ListingImage> {
        val copied = localUserRepository.copyPickerUriToPrivateDraftStorage(uri)
        val path = copied.getOrElse { err ->
            return MutationResult.Error(err.localizedMessage ?: "Selected image could not be read.")
        }
        return try {
            accountRepository.uploadImage(File(path))
        } finally {
            localUserRepository.deleteDraftImageFileIfUnused(path)
        }
    }

    fun clearListingActionMessage() {
        _myListingsActionMessage.value = null
    }

    fun loadNotifications() {
        val auth = authRepository.authState.value
        if (!auth.isSignedIn) return
        viewModelScope.launch {
            if (_notifications.value.isEmpty()) {
                _notificationsLoading.value = true
            }
            _notificationsError.value = null
            accountRepository.getNotifications()
                .onSuccess { items ->
                    val deleted = notificationPreferencesRepository.deletedNotificationIds.value
                    val deduped = items.distinctBy { it.id }.filterNot { it.id in deleted }
                    _notifications.value = deduped
                    localAccountCache.saveNotifications(auth.userScope, deduped)
                }
                .onFailure {
                    if (_notifications.value.isEmpty()) {
                        _notificationsError.value = it.localizedMessage
                    }
                }
            _notificationsLoading.value = false
        }
    }

    fun markNotificationRead(notificationId: String) {
        val scope = authRepository.authState.value.userScope
        val previous = _notifications.value
        if (previous.none { it.id == notificationId && !it.read }) return
        val updated = previous.map {
            if (it.id == notificationId) it.copy(read = true) else it
        }
        _notifications.value = updated
        localAccountCache.saveNotifications(scope, updated)
        viewModelScope.launch {
            when (val res = accountRepository.markNotificationRead(notificationId)) {
                is MutationResult.Success -> Unit
                is MutationResult.Error, is MutationResult.AmbiguousTimeout -> {
                    _notifications.value = previous
                    localAccountCache.saveNotifications(scope, previous)
                    _notificationsError.value = if (res is MutationResult.Error) res.message else "Timed out"
                }
            }
        }
    }

    fun markAllNotificationsRead() {
        val scope = authRepository.authState.value.userScope
        val unreadIds = _notifications.value.filter { !it.read }.map { it.id }
        notificationPreferencesRepository.markAllAnnouncementsRead()
        if (unreadIds.isEmpty()) return
        val updated = _notifications.value.map { it.copy(read = true) }
        _notifications.value = updated
        localAccountCache.saveNotifications(scope, updated)
        viewModelScope.launch {
            unreadIds.forEach { id ->
                accountRepository.markNotificationRead(id)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        if (notificationId.startsWith("ann_")) {
            notificationPreferencesRepository.deleteAnnouncement(notificationId.removePrefix("ann_"))
            return
        }
        val scope = authRepository.authState.value.userScope
        notificationPreferencesRepository.recordDeletedNotificationId(notificationId)
        val updated = _notifications.value.filterNot { it.id == notificationId }
        _notifications.value = updated
        localAccountCache.saveNotifications(scope, updated)
    }

    fun loadPayments() {
        val auth = authRepository.authState.value
        if (!auth.isSignedIn) return
        viewModelScope.launch {
            if (_payments.value.isEmpty()) {
                _paymentsLoading.value = true
            }
            _paymentsError.value = null
            accountRepository.getMyPayments()
                .onSuccess { items ->
                    _payments.value = items
                    localAccountCache.savePayments(auth.userScope, items)
                }
                .onFailure {
                    if (_payments.value.isEmpty()) {
                        _paymentsError.value = it.localizedMessage
                    }
                }
            _paymentsLoading.value = false
        }
    }

    fun loadPromotionsAndPricing() {
        val auth = authRepository.authState.value
        viewModelScope.launch {
            if (_promotionPricing.value == null) {
                _promotionPricing.value = localAccountCache.getPromotionPricing()
                    ?: marketplaceRepository.getCachedPromotionPricing()
            }
            if (_promotionPricing.value == null && _promotions.value.isEmpty()) {
                _promotionsLoading.value = true
            }
            _promotionsError.value = null
            accountRepository.getPromotionPricing()
                .onSuccess { pricing ->
                    _promotionPricing.value = pricing
                    localAccountCache.savePromotionPricing(pricing)
                }
                .onFailure {
                    if (_promotionPricing.value == null) {
                        _promotionsError.value = it.localizedMessage
                    }
                }
            if (auth.isSignedIn) {
                accountRepository.getMyPromotions()
                    .onSuccess { items ->
                        _promotions.value = items
                        localAccountCache.savePromotions(auth.userScope, items)
                    }
            }
            _promotionsLoading.value = false
        }
    }

    fun startK30ActivationPayment(rawPhone: String, payerType: String) {
        if (_paymentBusy.value) return
        if (_activePaymentId.value != null && _activePaymentStatus.value.equals("pending", ignoreCase = true)) {
            _activePaymentMessage.value = "A payment request is already pending. Tap Check Payment Status first."
            return
        }
        val normalized = PhoneValidator.normalizeZambianNumber(rawPhone)
        if (normalized == null) {
            _activePaymentMessage.value = "Enter a valid Zambian mobile money number (e.g. 969333123, 0969331234, or 260969331234)."
            return
        }
        val fee = uiState.value.verificationFeeZmw
        viewModelScope.launch {
            _paymentBusy.value = true
            _activePaymentPurpose.value = "Account Verification & Unlimited Posting"
            _activePaymentAmountZmw.value = fee
            _activePaymentMessage.value = "Please wait while we send your mobile money prompt."
            when (val res = accountRepository.createK30ActivationPayment(normalized, payerType)) {
                is MutationResult.Success -> {
                    _activePaymentId.value = res.data.paymentId
                    _activePaymentReference.value = res.data.referenceId
                    _activePaymentStatus.value = res.data.status ?: "pending"
                    _activePaymentMessage.value = res.data.message
                        ?: "Mobile money prompt sent. Approve on your phone, then check status."
                }
                is MutationResult.Error -> _activePaymentMessage.value = res.message
                is MutationResult.AmbiguousTimeout -> _activePaymentMessage.value = res.message
            }
            _paymentBusy.value = false
        }
    }

    fun startPromotionPayment(
        listingId: String,
        durationDays: Int,
        rawPhone: String,
        payerType: String,
        campaignFormat: String? = null
    ) {
        if (_paymentBusy.value) return
        if (_activePaymentId.value != null && _activePaymentStatus.value.equals("pending", ignoreCase = true)) {
            _activePaymentMessage.value = "A promotion payment is already pending. Tap Check Payment Status first."
            return
        }
        val normalized = PhoneValidator.normalizeZambianNumber(rawPhone)
        if (normalized == null) {
            _activePaymentMessage.value = "Enter a valid Zambian mobile money number (e.g. 969333123, 0969331234, or 260969331234)."
            return
        }
        val amount = _promotionPricing.value?.prices?.get(durationDays.toString())
        viewModelScope.launch {
            _paymentBusy.value = true
            _activePaymentPurpose.value = "Promote Listing · $durationDays Days"
            _activePaymentAmountZmw.value = amount
            _activePaymentMessage.value = "Please wait while we initiate your promotion payment."
            when (val res = accountRepository.createPromotionPayment(listingId, durationDays, normalized, payerType, campaignFormat)) {
                is MutationResult.Success -> {
                    _activePaymentId.value = res.data.paymentId
                    _activePaymentReference.value = res.data.referenceId
                    _activePaymentStatus.value = res.data.status ?: "pending"
                    _activePaymentMessage.value = res.data.message
                        ?: "Promotion prompt sent. Approve on the payer phone, then check status."
                }
                is MutationResult.Error -> _activePaymentMessage.value = res.message
                is MutationResult.AmbiguousTimeout -> _activePaymentMessage.value = res.message
            }
            _paymentBusy.value = false
        }
    }

    fun checkActivePaymentStatus() {
        val paymentId = _activePaymentId.value ?: return
        if (_paymentBusy.value) return
        viewModelScope.launch {
            _paymentBusy.value = true
            when (val res = accountRepository.checkPaymentStatus(paymentId)) {
                is MutationResult.Success -> {
                    val status = res.data.status ?: "pending"
                    _activePaymentStatus.value = status
                    _activePaymentMessage.value = when (status.lowercase()) {
                        "successful" -> {
                            loadProfile()
                            loadPromotionsAndPricing()
                            loadMyListings()
                            loadPayments()
                            loadNotifications()
                            "Payment confirmed! Your account or promotion is now active."
                        }
                        "pending" -> "Your payment is being processed. Approve the mobile money prompt, then check again."
                        "failed" -> "We could not confirm your payment.\nPlease try again or contact support."
                        "cancelled" -> "Payment was cancelled."
                        "expired" -> "Payment request expired. Please try again."
                        else -> res.data.message ?: "Status: $status"
                    }
                }
                is MutationResult.Error -> _activePaymentMessage.value = res.message
                is MutationResult.AmbiguousTimeout -> _activePaymentMessage.value = res.message
            }
            _paymentBusy.value = false
        }
    }

    fun clearActivePaymentState() {
        _activePaymentId.value = null
        _activePaymentReference.value = null
        _activePaymentStatus.value = null
        _activePaymentMessage.value = null
        _activePaymentPurpose.value = null
        _activePaymentAmountZmw.value = null
    }

    fun setThemeMode(modeName: String) {
        viewModelScope.launch {
            localUserRepository.updateSettings { it.copy(themeModeName = modeName) }
        }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            localUserRepository.updateSettings { it.copy(dataSaverEnabled = enabled) }
            val current = uiState.value.settings
            syncScheduler.schedulePeriodicSync(
                wifiOnly = current.wifiOnlyPrefetch,
                dataSaverEnabled = enabled
            )
        }
    }

    fun setWifiOnlyPrefetch(enabled: Boolean) {
        viewModelScope.launch {
            localUserRepository.updateSettings { it.copy(wifiOnlyPrefetch = enabled) }
            val current = uiState.value.settings
            syncScheduler.schedulePeriodicSync(
                wifiOnly = enabled,
                dataSaverEnabled = current.dataSaverEnabled
            )
        }
    }

    fun triggerBackgroundSync() {
        val current = uiState.value.settings
        syncScheduler.enqueueImmediateSync(
            force = true,
            respectWifiPref = current.wifiOnlyPrefetch,
            source = "manual"
        )
    }

    fun clearPublicCacheOnly() {
        viewModelScope.launch {
            marketplaceRepository.clearPublicCachePreservingDrafts()
            _cacheClearedMessage.value = "Public listing cache cleared. Your local drafts and favorites were preserved."
        }
    }

    fun clearCacheMessage() {
        _cacheClearedMessage.value = null
    }

    fun setGeneralNotificationsEnabled(enabled: Boolean) {
        notificationPreferencesRepository.updateGeneralEnabled(enabled)
    }

    fun setPromotionalNotificationsEnabled(enabled: Boolean) {
        notificationPreferencesRepository.updatePromotionalEnabled(enabled)
        val prefs = notificationPreferencesRepository.preferences.value
        syncScheduler.scheduleSmartEngagementWorker(prefs.smartEngagementEnabled && enabled)
    }

    fun setSmartEngagementEnabled(enabled: Boolean) {
        notificationPreferencesRepository.updateSmartEngagementEnabled(enabled)
        val prefs = notificationPreferencesRepository.preferences.value
        syncScheduler.scheduleSmartEngagementWorker(enabled && prefs.promotionalEnabled)
    }

    fun setMotivationalNotificationsEnabled(enabled: Boolean) {
        notificationPreferencesRepository.updateMotivationalEnabled(enabled)
        syncScheduler.scheduleMotivationalNotifications(enabled)
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        notificationPreferencesRepository.updateQuietHoursEnabled(enabled)
    }

    fun markAnnouncementRead(id: String) {
        notificationPreferencesRepository.markAnnouncementRead(id)
    }

    fun clearMyListingsError() {
        _myListingsError.value = null
    }

    fun seedDevFixtures() {
        val manager = devFixturesManager ?: return
        if (!manager.isFeatureAvailable) return
        viewModelScope.launch {
            val count = manager.seedFixtures()
            _cacheClearedMessage.value = "Loaded $count developer test listings across all categories."
        }
    }

    fun clearDevFixtures() {
        val manager = devFixturesManager ?: return
        viewModelScope.launch {
            manager.clearFixtures()
            _cacheClearedMessage.value = "Developer test listings cleared."
        }
    }

    fun clearNotificationsError() {
        _notificationsError.value = null
    }

    fun clearPaymentsError() {
        _paymentsError.value = null
    }

    fun clearPromotionsError() {
        _promotionsError.value = null
    }
}
