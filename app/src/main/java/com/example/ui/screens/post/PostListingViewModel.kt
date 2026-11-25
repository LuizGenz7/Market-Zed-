package com.example.ui.screens.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.PhoneValidator
import com.example.core.util.PriceFormatter
import com.example.data.auth.AuthRepository
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.CategoryDetails
import com.example.data.model.CategoryOption
import com.example.data.model.ContactPreferences
import com.example.data.model.ListingDraft
import com.example.data.model.ListingImage
import com.example.data.model.ListingLocation
import com.example.data.model.ProvinceLocations
import com.example.data.model.UserProfile
import com.example.data.remote.SubmitListingRequestDto
import com.example.data.repository.AccountRepository
import com.example.data.repository.InMemoryLocalAccountCache
import com.example.data.repository.LocalAccountCacheStore
import com.example.data.repository.LocalUserRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MutationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

sealed class DraftImageUploadState {
    data object Pending : DraftImageUploadState()
    data object Uploading : DraftImageUploadState()
    data class Uploaded(val image: ListingImage) : DraftImageUploadState()
    data class Failed(val error: String) : DraftImageUploadState()
}

data class DraftPhotoItem(
    val localPath: String,
    val uploadState: DraftImageUploadState = DraftImageUploadState.Pending
)

data class PostListingUiState(
    val isSignedIn: Boolean = false,
    val profileLoading: Boolean = false,
    val profileError: String? = null,
    val profile: UserProfile? = null,
    val draft: ListingDraft = ListingDraft(
        localUuid = UUID.randomUUID().toString(),
        userScope = "guest"
    ),
    val photos: List<DraftPhotoItem> = emptyList(),
    val savedDrafts: List<ListingDraft> = emptyList(),
    val categories: List<CategoryOption> = MarketZedCatalogue.categories,
    val provinces: List<ProvinceLocations> = MarketZedCatalogue.provinces,
    val validationErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val ambiguousSubmitMessage: String? = null,
    val publishedListingId: String? = null,
    val draftSavedNotice: String? = null,
    val localFreeWantedUsed: Boolean = false,
    val knownMyWantedCount: Int = 0,
    val wantedGateAcknowledged: Boolean = false,
    val verificationFeeZmw: Double = MarketZedCatalogue.DEFAULT_VERIFICATION_FEE_ZMW
) {
    val isWantedSelected: Boolean
        get() = draft.categoryId.equals("wanted", ignoreCase = true)

    val isVerifiedAccount: Boolean
        get() = profile?.hasVerifiedEntitlement == true

    val hasUsedFreeWantedPost: Boolean
        get() = (profile?.freeWantedPostUsed == true) ||
            (profile?.wantedPostsCount ?: 0) > 0 ||
            knownMyWantedCount > 0 ||
            localFreeWantedUsed

    val isFirstFreeWantedEligible: Boolean
        get() = isWantedSelected && !isVerifiedAccount && !hasUsedFreeWantedPost

    val needsVerificationForNextWanted: Boolean
        get() = isWantedSelected && !isVerifiedAccount && hasUsedFreeWantedPost

    val requiresK30ForSelectedCategory: Boolean
        get() = !isWantedSelected || hasUsedFreeWantedPost

    val isEligibleToPublishSelectedCategory: Boolean
        get() = if (isWantedSelected) {
            !needsVerificationForNextWanted
        } else {
            isVerifiedAccount
        }
}

class PostListingViewModel(
    private val accountRepository: AccountRepository,
    private val localUserRepository: LocalUserRepository,
    private val marketplaceRepository: MarketplaceRepository,
    private val authRepository: AuthRepository,
    private val localAccountCache: LocalAccountCacheStore = InMemoryLocalAccountCache()
) : ViewModel() {

    private val _profileLoading = MutableStateFlow(false)
    private val _profileError = MutableStateFlow<String?>(null)
    private val _profile = MutableStateFlow<UserProfile?>(null)
    private val _draft = MutableStateFlow(
        ListingDraft(
            localUuid = UUID.randomUUID().toString(),
            userScope = authRepository.authState.value.userScope
        )
    )
    private val _photos = MutableStateFlow<List<DraftPhotoItem>>(emptyList())
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _isSubmitting = MutableStateFlow(false)
    private val _submitError = MutableStateFlow<String?>(null)
    private val _ambiguousSubmitMessage = MutableStateFlow<String?>(null)
    private val _publishedListingId = MutableStateFlow<String?>(null)
    private val _draftSavedNotice = MutableStateFlow<String?>(null)
    private val _localFreeWantedUsed = MutableStateFlow(false)
    private val _knownMyWantedCount = MutableStateFlow(0)
    private val _wantedGateAcknowledged = MutableStateFlow(false)
    private val _verificationFeeZmw = MutableStateFlow(MarketZedCatalogue.DEFAULT_VERIFICATION_FEE_ZMW)

    private val publishMutex = Mutex()
    private var autosaveJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val savedDraftsFlow = authRepository.authState.flatMapLatest { auth ->
        localUserRepository.observeDrafts(auth.userScope)
    }

    val uiState: StateFlow<PostListingUiState> = combine(
        authRepository.authState,
        _profileLoading,
        _profileError,
        _profile,
        _draft,
        _photos,
        savedDraftsFlow,
        marketplaceRepository.observeCategories(),
        marketplaceRepository.observeLocations(),
        _validationErrors,
        _isSubmitting,
        _submitError,
        _ambiguousSubmitMessage,
        _publishedListingId,
        _draftSavedNotice,
        _localFreeWantedUsed,
        _knownMyWantedCount,
        _wantedGateAcknowledged,
        _verificationFeeZmw
    ) { arr ->
        val auth = arr[0] as com.example.data.auth.AuthUserState
        @Suppress("UNCHECKED_CAST")
        val photosList = arr[5] as List<DraftPhotoItem>
        @Suppress("UNCHECKED_CAST")
        val draftsList = arr[6] as List<ListingDraft>
        @Suppress("UNCHECKED_CAST")
        val cats = arr[7] as List<CategoryOption>
        @Suppress("UNCHECKED_CAST")
        val provs = arr[8] as List<ProvinceLocations>
        @Suppress("UNCHECKED_CAST")
        val valErrors = arr[9] as Map<String, String>

        PostListingUiState(
            isSignedIn = auth.isSignedIn,
            profileLoading = arr[1] as Boolean,
            profileError = arr[2] as String?,
            profile = arr[3] as UserProfile?,
            draft = arr[4] as ListingDraft,
            photos = photosList,
            savedDrafts = draftsList,
            categories = cats,
            provinces = provs,
            validationErrors = valErrors,
            isSubmitting = arr[10] as Boolean,
            submitError = arr[11] as String?,
            ambiguousSubmitMessage = arr[12] as String?,
            publishedListingId = arr[13] as String?,
            draftSavedNotice = arr[14] as String?,
            localFreeWantedUsed = arr[15] as Boolean,
            knownMyWantedCount = arr[16] as Int,
            wantedGateAcknowledged = arr[17] as Boolean,
            verificationFeeZmw = arr[18] as Double
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PostListingUiState()
    )

    init {
        viewModelScope.launch {
            authRepository.authState.collect { auth ->
                _draft.update { it.copy(userScope = auth.userScope) }
                _wantedGateAcknowledged.value = false
                if (auth.isSignedIn) {
                    _localFreeWantedUsed.value = localAccountCache.getLocalFreeWantedPostUsed(auth.userScope)
                    val cachedWantedCount = localAccountCache.getMyListings(auth.userScope).count { it.isWanted }
                    _knownMyWantedCount.value = cachedWantedCount
                    if (cachedWantedCount > 0) {
                        localAccountCache.markLocalFreeWantedPostUsed(auth.userScope)
                        _localFreeWantedUsed.value = true
                    }
                    val cachedFee = localAccountCache.getPromotionPricing()
                        ?.verificationFeeOrDefault(MarketZedCatalogue.DEFAULT_VERIFICATION_FEE_ZMW)
                    if (cachedFee != null) {
                        _verificationFeeZmw.value = cachedFee
                    }
                    val cachedProfile = localAccountCache.getProfile(auth.userScope)
                    if (cachedProfile != null) {
                        _profile.value = cachedProfile
                        cachedProfile.verificationFeeZmw?.let { _verificationFeeZmw.value = it }
                        if (cachedProfile.freeWantedPostUsed || cachedProfile.wantedPostsCount > 0) {
                            localAccountCache.markLocalFreeWantedPostUsed(auth.userScope)
                            _localFreeWantedUsed.value = true
                        }
                        _draft.update { d ->
                            val prov = d.province.ifBlank { cachedProfile.province ?: "Lusaka" }
                            val defaultDistrict = MarketZedCatalogue.districtsForProvince(prov).firstOrNull().orEmpty()
                            d.copy(
                                province = prov,
                                district = d.district.ifBlank { defaultDistrict },
                                town = d.town.ifBlank { cachedProfile.town ?: "Lusaka" }
                            )
                        }
                    }
                    refreshProfileAccess()
                } else {
                    _profile.value = null
                    _profileError.value = null
                    _localFreeWantedUsed.value = false
                    _knownMyWantedCount.value = 0
                }
            }
        }
    }

    fun refreshProfileAccess() {
        val auth = authRepository.authState.value
        if (!auth.isSignedIn) return
        viewModelScope.launch {
            if (_profile.value == null) {
                _profileLoading.value = true
            }
            _profileError.value = null
            accountRepository.getPromotionPricing().onSuccess { pricing ->
                localAccountCache.savePromotionPricing(pricing)
                _verificationFeeZmw.value = pricing.verificationFeeOrDefault(MarketZedCatalogue.DEFAULT_VERIFICATION_FEE_ZMW)
            }
            accountRepository.getProfile()
                .onSuccess { p ->
                    _profile.value = p
                    localAccountCache.saveProfile(auth.userScope, p)
                    p.verificationFeeZmw?.let { _verificationFeeZmw.value = it }
                    if (p.freeWantedPostUsed || p.wantedPostsCount > 0) {
                        localAccountCache.markLocalFreeWantedPostUsed(auth.userScope)
                        _localFreeWantedUsed.value = true
                    }
                    _draft.update { d ->
                        val prov = d.province.ifBlank { p.province ?: "Lusaka" }
                        val defaultDistrict = MarketZedCatalogue.districtsForProvince(prov).firstOrNull().orEmpty()
                        d.copy(
                            province = prov,
                            district = d.district.ifBlank { defaultDistrict },
                            town = d.town.ifBlank { p.town ?: "Lusaka" }
                        )
                    }
                }
                .onFailure { err ->
                    if (_profile.value == null) {
                        _profileError.value = err.localizedMessage ?: "Could not verify posting access."
                    }
                }
            // Cross-check owned listings to prevent pre-existing accounts with Wanted posts bypassing free limit
            if (!_localFreeWantedUsed.value && (_profile.value?.wantedPostsCount ?: 0) == 0) {
                accountRepository.getMyListings().onSuccess { myItems ->
                    localAccountCache.saveMyListings(auth.userScope, myItems)
                    val wantedCount = myItems.count { it.isWanted }
                    _knownMyWantedCount.value = wantedCount
                    if (wantedCount > 0) {
                        localAccountCache.markLocalFreeWantedPostUsed(auth.userScope)
                        _localFreeWantedUsed.value = true
                    }
                }
            }
            _profileLoading.value = false
        }
    }

    fun acknowledgeWantedGate() {
        _wantedGateAcknowledged.value = true
    }

    fun setStep(step: Int) {
        val isWanted = _draft.value.categoryId.equals("wanted", ignoreCase = true)
        val normalized = when {
            isWanted && step == 1 && _draft.value.step < 1 -> 2
            isWanted && step == 1 && _draft.value.step > 1 -> 0
            isWanted && step == 1 -> 2
            else -> step.coerceIn(0, 3)
        }
        _draft.update { it.copy(step = normalized) }
        scheduleAutosave()
    }

    fun selectCategory(categoryId: String) {
        val prev = _draft.value.categoryId
        if (prev == categoryId) return
        val cat = MarketZedCatalogue.categoryById(categoryId)
        val isWanted = categoryId.equals("wanted", ignoreCase = true)
        if (isWanted) {
            _photos.value.forEach { localUserRepository.deleteDraftImageFileIfUnused(it.localPath) }
            _photos.value = emptyList()
        }
        _draft.update {
            it.copy(
                categoryId = categoryId,
                subcategory = cat?.subcategories?.firstOrNull().orEmpty(),
                categoryDetails = CategoryDetails(),
                step = if (isWanted && it.step == 1) 2 else it.step,
                localImagePaths = if (isWanted) emptyList() else it.localImagePaths,
                uploadedImages = if (isWanted) emptyList() else it.uploadedImages
            )
        }
        _wantedGateAcknowledged.value = false
        _validationErrors.update { it - "category" - "subcategory" - "images" }
        scheduleAutosave()
    }

    fun updateDraft(transform: (ListingDraft) -> ListingDraft) {
        _draft.update(transform)
        _submitError.value = null
        _ambiguousSubmitMessage.value = null
        scheduleAutosave()
    }

    fun startNewDraft() {
        _publishedListingId.value = null
        _submitError.value = null
        _ambiguousSubmitMessage.value = null
        _validationErrors.value = emptyMap()
        _wantedGateAcknowledged.value = false
        _photos.value = emptyList()
        val p = _profile.value
        val prov = p?.province?.ifBlank { null } ?: "Lusaka"
        _draft.value = ListingDraft(
            localUuid = UUID.randomUUID().toString(),
            userScope = authRepository.authState.value.userScope,
            province = prov,
            district = MarketZedCatalogue.districtsForProvince(prov).firstOrNull().orEmpty(),
            town = p?.town?.ifBlank { null } ?: "Lusaka"
        )
    }

    fun loadSavedDraft(draft: ListingDraft) {
        _publishedListingId.value = null
        _submitError.value = null
        _ambiguousSubmitMessage.value = null
        _validationErrors.value = emptyMap()
        val isWanted = draft.categoryId.equals("wanted", ignoreCase = true)
        val normalizedStep = if (isWanted && draft.step == 1) 2 else draft.step
        val cleanDraft = if (isWanted) {
            draft.copy(step = normalizedStep, localImagePaths = emptyList(), uploadedImages = emptyList())
        } else {
            draft.copy(step = normalizedStep)
        }
        _draft.value = cleanDraft
        _photos.value = if (isWanted) {
            emptyList()
        } else {
            cleanDraft.localImagePaths.mapIndexed { idx, path ->
                val uploaded = cleanDraft.uploadedImages.getOrNull(idx)
                DraftPhotoItem(
                    localPath = path,
                    uploadState = if (uploaded != null && uploaded.url.isNotBlank()) {
                        DraftImageUploadState.Uploaded(uploaded)
                    } else {
                        DraftImageUploadState.Pending
                    }
                )
            }
        }
    }

    fun deleteSavedDraft(localUuid: String) {
        viewModelScope.launch {
            val target = localUserRepository.getDraft(localUuid)
            target?.localImagePaths?.forEach { localUserRepository.deleteDraftImageFileIfUnused(it) }
            localUserRepository.deleteDraft(localUuid)
            if (_draft.value.localUuid == localUuid) {
                startNewDraft()
            }
        }
    }

    fun saveDraftNow() {
        autosaveJob?.cancel()
        viewModelScope.launch {
            flushDraftToRoom()
            _draftSavedNotice.value = "Draft saved on this device"
        }
    }

    fun clearDraftSavedNotice() {
        _draftSavedNotice.value = null
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(500)
            flushDraftToRoom()
        }
    }

    private suspend fun flushDraftToRoom() {
        val current = _draft.value
        val hasMeaningfulContent = current.title.isNotBlank() ||
            current.description.isNotBlank() ||
            current.localImagePaths.isNotEmpty() ||
            current.price.isNotBlank()
        if (!hasMeaningfulContent) return

        val isWanted = current.categoryId.equals("wanted", ignoreCase = true)
        val uploaded = if (isWanted) {
            emptyList()
        } else {
            _photos.value.mapNotNull { (it.uploadState as? DraftImageUploadState.Uploaded)?.image }
        }
        localUserRepository.saveDraft(
            current.copy(
                userScope = authRepository.authState.value.userScope,
                localImagePaths = if (isWanted) emptyList() else _photos.value.map { it.localPath },
                uploadedImages = uploaded,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun addPickedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (_draft.value.categoryId.equals("wanted", ignoreCase = true)) {
            _submitError.value = "Wanted requests are text-only and do not use photos."
            return
        }
        viewModelScope.launch {
            val currentCount = _photos.value.size
            val remainingSlots = (10 - currentCount).coerceAtLeast(0)
            val selected = uris.take(remainingSlots)
            val newItems = mutableListOf<DraftPhotoItem>()
            var lastErr: String? = null
            for (uri in selected) {
                val res = localUserRepository.copyPickerUriToPrivateDraftStorage(uri)
                res.onSuccess { path ->
                    newItems.add(DraftPhotoItem(localPath = path))
                }.onFailure { err ->
                    lastErr = err.localizedMessage
                }
            }
            if (newItems.isNotEmpty()) {
                _photos.update { it + newItems }
                _draft.update { d -> d.copy(localImagePaths = _photos.value.map { p -> p.localPath }) }
                _validationErrors.update { it - "images" }
                flushDraftToRoom()
                uploadPendingImages()
            }
            if (lastErr != null) {
                _submitError.value = lastErr
            }
        }
    }

    fun removePhoto(localPath: String) {
        _photos.update { list -> list.filterNot { it.localPath == localPath } }
        localUserRepository.deleteDraftImageFileIfUnused(localPath)
        _draft.update { d -> d.copy(localImagePaths = _photos.value.map { it.localPath }) }
        scheduleAutosave()
    }

    fun retryPhotoUpload(localPath: String) {
        if (_draft.value.categoryId.equals("wanted", ignoreCase = true)) return
        viewModelScope.launch {
            uploadSingleImage(localPath)
        }
    }

    private suspend fun uploadPendingImages() {
        if (_draft.value.categoryId.equals("wanted", ignoreCase = true)) return
        val pendingPaths = _photos.value
            .filter { it.uploadState is DraftImageUploadState.Pending || it.uploadState is DraftImageUploadState.Failed }
            .map { it.localPath }
        for (path in pendingPaths) {
            uploadSingleImage(path)
        }
    }

    private suspend fun uploadSingleImage(localPath: String) {
        _photos.update { list ->
            list.map { if (it.localPath == localPath) it.copy(uploadState = DraftImageUploadState.Uploading) else it }
        }
        val res = accountRepository.uploadImage(File(localPath))
        _photos.update { list ->
            list.map { item ->
                if (item.localPath != localPath) item
                else when (res) {
                    is MutationResult.Success -> item.copy(uploadState = DraftImageUploadState.Uploaded(res.data))
                    is MutationResult.Error -> item.copy(uploadState = DraftImageUploadState.Failed(res.message))
                    is MutationResult.AmbiguousTimeout -> item.copy(uploadState = DraftImageUploadState.Failed(res.message))
                }
            }
        }
        flushDraftToRoom()
    }

    fun validateStep(step: Int): Boolean {
        val d = _draft.value
        val errors = mutableMapOf<String, String>()
        val isWanted = d.categoryId.equals("wanted", ignoreCase = true)
        when (step) {
            0 -> {
                if (d.categoryId.isBlank()) errors["category"] = "Choose a category."
                if (d.subcategory.trim().length < 2) errors["subcategory"] = "Choose a subcategory."
                val titleLen = d.title.trim().length
                if (titleLen < 4 || titleLen > 120) {
                    errors["title"] = "Title must be between 4 and 120 characters."
                }
                val descLen = d.description.trim().length
                if (descLen < 20 || descLen > 5000) {
                    errors["description"] = "Description must be between 20 and 5,000 characters."
                }
                if (d.price.isNotBlank() && PriceFormatter.parsePriceInput(d.price) == null) {
                    errors["price"] = "Enter a valid non-negative ZMW amount or leave blank."
                }
                MarketZedCatalogue.validatePropertyAndLandPolicy(
                    categoryId = d.categoryId,
                    subcategory = d.subcategory,
                    title = d.title,
                    description = d.description
                )?.let { policyErr ->
                    errors["policy"] = policyErr
                }
            }
            1 -> {
                if (isWanted) {
                    if (_photos.value.isNotEmpty()) {
                        errors["images"] = "Wanted requests are text-only and cannot include photos."
                    }
                } else {
                    val cat = MarketZedCatalogue.categoryById(d.categoryId)
                    if (cat?.requiresImage == true && _photos.value.isEmpty()) {
                        errors["images"] = "Add at least 1 photo for ${cat.name} listings."
                    }
                    if (_photos.value.size > 10) {
                        errors["images"] = "Maximum 10 photos allowed."
                    }
                }
            }
            2 -> {
                if (d.province.isBlank()) errors["province"] = "Select a province."
                if (d.town.isBlank()) errors["town"] = "Select or enter a town."
                val phone = d.contactPreferences.phone?.trim().orEmpty()
                val wa = d.contactPreferences.whatsapp?.trim().orEmpty()
                val email = d.contactPreferences.email?.trim().orEmpty()
                if (phone.isEmpty() && wa.isEmpty() && email.isEmpty()) {
                    errors["contact"] = "Provide at least one contact method (Phone, WhatsApp, or Email)."
                }
                val normalizedPhone = if (phone.isNotEmpty()) PhoneValidator.normalizeZambianNumber(phone) else null
                if (phone.isNotEmpty() && normalizedPhone == null) {
                    errors["phone"] = "Enter a Zambian number (e.g. 969333123, 0969331234, or 260969331234)."
                }
                val normalizedWa = if (wa.isNotEmpty()) PhoneValidator.normalizeZambianNumber(wa) else null
                if (wa.isNotEmpty() && normalizedWa == null) {
                    errors["whatsapp"] = "Enter a Zambian number (e.g. 969333123, 0969331234, or 260969331234)."
                }
                if (email.isNotEmpty() && !PhoneValidator.isValidEmail(email)) {
                    errors["email"] = "Enter a valid email address."
                }
                if (normalizedPhone != null && normalizedPhone != phone || normalizedWa != null && normalizedWa != wa) {
                    _draft.update {
                        it.copy(
                            contactPreferences = it.contactPreferences.copy(
                                phone = normalizedPhone ?: it.contactPreferences.phone,
                                whatsapp = normalizedWa ?: it.contactPreferences.whatsapp
                            )
                        )
                    }
                }
            }
            3 -> {
                if (!d.ownershipDeclarationAccepted) {
                    errors["declaration"] = "You must confirm the ownership & lawful authority declaration."
                }
            }
        }
        _validationErrors.value = errors
        return errors.isEmpty()
    }

    fun submitListing() {
        if (_isSubmitting.value || _ambiguousSubmitMessage.value != null) return
        if (!validateStep(0) || !validateStep(1) || !validateStep(2) || !validateStep(3)) {
            _validationErrors.value["policy"]?.let { policyErr ->
                _submitError.value = policyErr
            }
            return
        }

        val snapshotState = uiState.value
        if (snapshotState.needsVerificationForNextWanted) {
            _submitError.value =
                "You have already used your 1 free Wanted post. Verify your account (${PriceFormatter.formatKwachaAmount(snapshotState.verificationFeeZmw)}) to unlock unlimited Wanted posts."
            return
        }
        if (!snapshotState.isWantedSelected && !snapshotState.isVerifiedAccount) {
            _submitError.value =
                "One-time account verification (${PriceFormatter.formatKwachaAmount(snapshotState.verificationFeeZmw)}) is required before publishing in this category."
            return
        }

        val currentPhotos = if (snapshotState.isWantedSelected) emptyList() else _photos.value
        val hasUploading = currentPhotos.any { it.uploadState is DraftImageUploadState.Uploading }
        if (hasUploading) {
            _submitError.value = "Please wait for image uploads to finish."
            return
        }

        viewModelScope.launch {
            publishMutex.withLock {
                if (_isSubmitting.value) return@withLock
                _isSubmitting.value = true
                _submitError.value = null
                _ambiguousSubmitMessage.value = null

                try {
                    // Re-verify Wanted entitlement inside Mutex right before network call
                    val d = _draft.value
                    val isWanted = d.categoryId.equals("wanted", ignoreCase = true)
                    val verifiedNow = _profile.value?.hasVerifiedEntitlement == true
                    val freeUsedNow = (_profile.value?.freeWantedPostUsed == true) ||
                        (_profile.value?.wantedPostsCount ?: 0) > 0 ||
                        _knownMyWantedCount.value > 0 ||
                        _localFreeWantedUsed.value
                    if (isWanted && !verifiedNow && freeUsedNow) {
                        _submitError.value = "You have used your 1 free Wanted post. Verify your account to unlock unlimited Wanted posts."
                        return@withLock
                    }

                    if (!isWanted && currentPhotos.any { it.uploadState !is DraftImageUploadState.Uploaded }) {
                        uploadPendingImages()
                    }

                    val failedPhotos = if (isWanted) emptyList() else _photos.value.filter { it.uploadState !is DraftImageUploadState.Uploaded }
                    if (failedPhotos.isNotEmpty()) {
                        _submitError.value = "Some photos failed to upload. Retry or remove failed photos before publishing."
                        return@withLock
                    }

                    val uploadedImages = if (isWanted) {
                        emptyList()
                    } else {
                        _photos.value.mapNotNull { (it.uploadState as? DraftImageUploadState.Uploaded)?.image }
                    }

                    val payload = SubmitListingRequestDto(
                        categoryId = d.categoryId,
                        subcategory = d.subcategory.trim(),
                        title = d.title.trim(),
                        description = d.description.trim(),
                        price = d.price.trim().takeIf { it.isNotEmpty() },
                        location = ListingLocation(
                            province = d.province.trim(),
                            town = d.town.trim(),
                            district = d.district.trim(),
                            nearbyArea = d.nearbyArea.trim().takeIf { it.isNotEmpty() }
                        ),
                        images = uploadedImages,
                        contactPreferences = ContactPreferences(
                            phone = d.contactPreferences.phone?.trim()?.takeIf { it.isNotEmpty() }
                                ?.let { PhoneValidator.normalizeZambianNumber(it) ?: it },
                            whatsapp = d.contactPreferences.whatsapp?.trim()?.takeIf { it.isNotEmpty() }
                                ?.let { PhoneValidator.normalizeZambianNumber(it) ?: it },
                            email = d.contactPreferences.email?.trim()?.takeIf { it.isNotEmpty() }
                        ),
                        categoryDetails = CategoryDetails(
                            condition = d.categoryDetails.condition?.trim()?.takeIf { it.isNotEmpty() },
                            bedrooms = d.categoryDetails.bedrooms?.trim()?.takeIf { it.isNotEmpty() },
                            bathrooms = d.categoryDetails.bathrooms?.trim()?.takeIf { it.isNotEmpty() },
                            amenities = d.categoryDetails.amenities?.trim()?.takeIf { it.isNotEmpty() },
                            employerName = d.categoryDetails.employerName?.trim()?.takeIf { it.isNotEmpty() },
                            employmentType = d.categoryDetails.employmentType?.trim()?.takeIf { it.isNotEmpty() },
                            swapFor = d.categoryDetails.swapFor?.trim()?.takeIf { it.isNotEmpty() },
                            serviceArea = d.categoryDetails.serviceArea?.trim()?.takeIf { it.isNotEmpty() },
                            wantedDetails = d.categoryDetails.wantedDetails?.trim()?.takeIf { it.isNotEmpty() },
                            brand = d.categoryDetails.brand?.trim()?.takeIf { it.isNotEmpty() },
                            model = d.categoryDetails.model?.trim()?.takeIf { it.isNotEmpty() },
                            specs = d.categoryDetails.specs
                                ?.filterKeys { it.isNotBlank() && !it.startsWith("__") }
                                ?.mapValues { it.value.trim() }
                                ?.filterValues { it.isNotEmpty() }
                                ?.takeIf { it.isNotEmpty() }
                        ),
                        ownershipDeclarationAccepted = true,
                        ownershipDeclarationVersion = MarketZedCatalogue.OWNERSHIP_DECLARATION_VERSION
                    )

                    when (val res = accountRepository.submitListing(payload)) {
                        is MutationResult.Success -> {
                            val createdId = res.data.listingId ?: "published"
                            _publishedListingId.value = createdId
                            if (isWanted) {
                                // Consume free Wanted entitlement ONLY after server-confirmed success
                                localAccountCache.markLocalFreeWantedPostUsed(d.userScope)
                                _localFreeWantedUsed.value = true
                                _knownMyWantedCount.update { it + 1 }
                                _profile.update { currentProfile ->
                                    currentProfile?.copy(
                                        freeWantedPostUsed = true,
                                        wantedPostsCount = currentProfile.wantedPostsCount + 1
                                    )?.also { updated ->
                                        localAccountCache.saveProfile(d.userScope, updated)
                                    }
                                }
                            }
                            // Delete local draft only after server confirms publication
                            localUserRepository.deleteDraft(d.localUuid)
                            marketplaceRepository.refreshHomeFeeds(force = true)
                        }
                        is MutationResult.Error -> {
                            // Never consume freeWantedPostUsed on error
                            flushDraftToRoom()
                            _submitError.value = res.message
                        }
                        is MutationResult.AmbiguousTimeout -> {
                            // Never consume freeWantedPostUsed on ambiguous timeout
                            flushDraftToRoom()
                            _ambiguousSubmitMessage.value = res.message
                        }
                    }
                } finally {
                    _isSubmitting.value = false
                }
            }
        }
    }
}
