package com.example.ui.screens.account

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.BuildConfig
import com.example.core.util.DateUtils
import com.example.core.util.PhoneValidator
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.repository.MutationResult
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HeroPaymentAmountCard
import com.example.ui.components.LipilaPaymentMethodSummaryCard
import com.example.ui.components.ListRowSkeletonSection
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.PaymentPhaseStatusCard
import com.example.ui.components.PaymentResultDialog
import com.example.ui.components.PaymentSecurityReassuranceBanner
import com.example.ui.components.PaymentUiStatePhase
import com.example.ui.components.SecondaryPageScaffold
import com.example.ui.components.StatusBadgePill
import com.example.ui.components.SubtleRefreshBar
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onSaveProfile: (String, String, String, String, String, String, String, String, String) -> Unit,
    onClearMessages: () -> Unit
) {
    BackHandler(onBack = onBack)
    val p = state.profile
    var firstName by remember(p) { mutableStateOf(p?.firstName.orEmpty()) }
    var lastName by remember(p) { mutableStateOf(p?.lastName.orEmpty()) }
    var contactNumber by remember(p) {
        mutableStateOf(PhoneValidator.autoFixZambianInput(p?.contactNumber.orEmpty()))
    }
    var dateOfBirth by remember(p) { mutableStateOf(p?.dateOfBirth.orEmpty()) }
    var whatsapp by remember(p) {
        mutableStateOf(PhoneValidator.autoFixZambianInput(p?.whatsappNumber.orEmpty()))
    }
    var altContact by remember(p) {
        mutableStateOf(PhoneValidator.autoFixZambianInput(p?.alternativeContact.orEmpty()))
    }
    var accountType by remember(p) { mutableStateOf(p?.accountType ?: "individual") }
    var province by remember(p) { mutableStateOf(p?.province ?: "Lusaka") }
    var town by remember(p) { mutableStateOf(p?.town ?: "Lusaka") }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.profileError) {
        val err = state.profileError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showError(err)
        onClearMessages()
    }
    LaunchedEffect(state.profileSaveMessage) {
        val msg = state.profileSaveMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showSuccess(msg)
        onClearMessages()
    }

    SecondaryPageScaffold(
        title = "Edit Profile",
        onBack = { onClearMessages(); onBack() },
        backButtonTestTag = "profile_back_button"
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // NO biography field per authoritative amendment!
            MarketZedOutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name *") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_user),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile_first_name")
            )
            MarketZedOutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name *") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_user),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile_last_name")
            )
            val contactPreview = PhoneValidator.normalizeZambianNumber(contactNumber)
                ?.takeIf { it != contactNumber.trim() }
            MarketZedOutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact Number * (e.g. 969333123 or 0969331234)") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_phone),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                supportingText = contactPreview?.let { fixed -> { Text("Auto-formats to $fixed") } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && contactNumber.isNotBlank()) {
                            contactNumber = PhoneValidator.autoFixZambianInput(contactNumber)
                        }
                    }
            )
            val waPreview = PhoneValidator.normalizeZambianNumber(whatsapp)
                ?.takeIf { it != whatsapp.trim() }
            MarketZedOutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp Number (optional, e.g. 969333123 or 0969331234)") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_message_circle),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                supportingText = waPreview?.let { fixed -> { Text("Auto-formats to $fixed") } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && whatsapp.isNotBlank()) {
                            whatsapp = PhoneValidator.autoFixZambianInput(whatsapp)
                        }
                    }
            )
            MarketZedOutlinedTextField(
                value = altContact,
                onValueChange = { altContact = it },
                label = { Text("Alternative Contact (optional)") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_phone_call),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && altContact.isNotBlank()) {
                            altContact = PhoneValidator.autoFixZambianInput(altContact)
                        }
                    }
            )
            MarketZedOutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Date of Birth (YYYY-MM-DD, optional)") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Account Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = accountType == "individual",
                    onClick = { accountType = "individual" },
                    label = { Text("Individual") }
                )
                FilterChip(
                    selected = accountType == "business",
                    onClick = { accountType = "business" },
                    label = { Text("Business") }
                )
            }
            MarketZedOutlinedTextField(
                value = province,
                onValueChange = { province = it },
                label = { Text("Province") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            MarketZedOutlinedTextField(
                value = town,
                onValueChange = { town = it },
                label = { Text("Town") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_map_pinned),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val fixedContact = PhoneValidator.autoFixZambianInput(contactNumber)
                    val fixedWa = PhoneValidator.autoFixZambianInput(whatsapp)
                    val fixedAlt = PhoneValidator.autoFixZambianInput(altContact)
                    contactNumber = fixedContact
                    whatsapp = fixedWa
                    altContact = fixedAlt
                    onSaveProfile(firstName, lastName, fixedContact, dateOfBirth, fixedWa, fixedAlt, accountType, province, town)
                },
                enabled = !state.profileLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                modifier = Modifier.fillMaxWidth().testTag("profile_save_button")
            ) {
                Text(if (state.profileLoading) "Saving..." else "Save Profile")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationPaymentScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onStartPayment: (phone: String, payerType: String) -> Unit,
    onCheckStatus: () -> Unit,
    onClearPaymentState: () -> Unit
) {
    BackHandler(onBack = onBack)
    var phone by remember {
        mutableStateOf(PhoneValidator.autoFixZambianInput(state.profile?.contactNumber.orEmpty()))
    }
    var payerType by remember { mutableStateOf("self") }
    var showResultDialog by remember { mutableStateOf(false) }
    val snackbar = LocalTopSnackbarController.current
    val feeZmw = state.verificationFeeZmw
    val feeFormatted = PriceFormatter.formatKwachaAmount(feeZmw)

    val phase = state.resolvePaymentPhase(
        purposeLabel = "Account Verification & Unlimited Posting",
        fallbackAmountZmw = feeZmw,
        successDetail = "Your account verification is now active across all categories and Wanted requests."
    )

    LaunchedEffect(state.activePaymentStatus, state.activePaymentId) {
        val status = state.activePaymentStatus?.trim()?.lowercase()
        if (!state.activePaymentId.isNullOrBlank() && status in setOf("successful", "failed", "cancelled", "expired")) {
            showResultDialog = true
        }
    }

    LaunchedEffect(state.activePaymentMessage, state.activePaymentId, state.activePaymentStatus) {
        val msg = state.activePaymentMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        when {
            state.activePaymentStatus.equals("successful", ignoreCase = true) ->
                snackbar?.showSuccess(msg)
            state.activePaymentId == null && !state.paymentBusy -> {
                snackbar?.showError(msg)
            }
            state.activePaymentStatus in setOf("failed", "cancelled", "expired") ->
                snackbar?.showWarning(msg)
        }
    }

    SecondaryPageScaffold(
        title = "Account Verification",
        onBack = onBack,
        backButtonTestTag = "activation_back_button"
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroPaymentAmountCard(
                purposeBadge = "ONE-TIME VERIFICATION",
                purposeTitle = "Account Verification & Unlimited Posting",
                amountZmw = feeZmw,
                shortExplanation = "One-time mobile money fee unlocks all 7 marketplace categories plus unlimited Wanted requests."
            )

            if (state.profile?.hasVerifiedEntitlement == true) {
                PaymentPhaseStatusCard(
                    phase = PaymentUiStatePhase.Successful(
                        paymentId = state.profile.verificationPaymentId,
                        amountZmw = feeZmw,
                        purposeTitle = "Account Verification",
                        headline = "Account Verified",
                        detail = "Your verification and posting access are active. No additional fee is required."
                    ),
                    onDone = onBack
                )
            } else {
                LipilaPaymentMethodSummaryCard(
                    instructions = "Approve the $feeFormatted Lipila Mobile Money prompt on the payer handset, then tap Check Payment Status."
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = payerType == "self",
                        onClick = { payerType = "self" },
                        label = { Text("My mobile money") }
                    )
                    FilterChip(
                        selected = payerType == "other",
                        onClick = { payerType = "other" },
                        label = { Text("Someone else's number") }
                    )
                }

                val activationPhonePreview = PhoneValidator.normalizeZambianNumber(phone)
                    ?.takeIf { it != phone.trim() }
                MarketZedOutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Payer Mobile Money Number (e.g. 0969331234)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_smartphone),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    supportingText = activationPhonePreview?.let { fixed -> { Text("Auto-formats to $fixed") } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            if (!focus.isFocused && phone.isNotBlank()) {
                                phone = PhoneValidator.autoFixZambianInput(phone)
                            }
                        }
                        .testTag("activation_phone_input")
                )

                PaymentPhaseStatusCard(
                    phase = phase,
                    onCheckStatus = if (state.activePaymentId != null && !state.paymentBusy) onCheckStatus else null,
                    onRetry = {
                        onClearPaymentState()
                        val fixed = PhoneValidator.autoFixZambianInput(phone)
                        phone = fixed
                        onStartPayment(fixed, payerType)
                    },
                    onDone = onBack
                )

                if (state.activePaymentId == null || phase is PaymentUiStatePhase.Failed || phase is PaymentUiStatePhase.Cancelled) {
                    Button(
                        onClick = {
                            onClearPaymentState()
                            val fixedPhone = PhoneValidator.autoFixZambianInput(phone)
                            phone = fixedPhone
                            onStartPayment(fixedPhone, payerType)
                        },
                        enabled = !state.paymentBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("activation_pay_button")
                    ) {
                        if (state.paymentBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending Prompt...", color = Color.White)
                        } else {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Now · $feeFormatted", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                PaymentSecurityReassuranceBanner()
            }
        }
    }

    if (showResultDialog && phase !is PaymentUiStatePhase.Idle && phase !is PaymentUiStatePhase.Processing) {
        PaymentResultDialog(
            phase = phase,
            onDismiss = { showResultDialog = false },
            onCheckStatus = onCheckStatus,
            onRetry = {
                onClearPaymentState()
                val fixed = PhoneValidator.autoFixZambianInput(phone)
                phone = fixed
                onStartPayment(fixed, payerType)
            },
            onSuccessContinue = {
                onClearPaymentState()
                onBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onLoadMyListings: () -> Unit,
    onOpenListing: (String) -> Unit,
    onManageListing: (String, String) -> Unit,
    onReviseListing: (String, Map<String, Any?>, (Boolean, String) -> Unit) -> Unit,
    onUploadEditImage: suspend (Uri) -> MutationResult<ListingImage>,
    onPromoteListing: (Listing) -> Unit,
    onPostListing: () -> Unit = {},
    onClearActionMessage: () -> Unit,
    onClearListingsError: () -> Unit,
    onRequireSignInAgain: () -> Unit = {},
    onClearSessionExpiredPrompt: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { onLoadMyListings() }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.myListingsActionMessage) {
        val msg = state.myListingsActionMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (msg.contains("updated", ignoreCase = true) || msg.contains("saved", ignoreCase = true)) {
            snackbar?.showSuccess(msg)
        } else {
            snackbar?.showWarning(msg)
        }
        onClearActionMessage()
    }
    LaunchedEffect(state.myListingsError) {
        val err = state.myListingsError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showError(err, actionLabel = "Retry", onAction = onLoadMyListings)
        onClearListingsError()
    }

    var selectedFilter by remember { mutableStateOf("all") }
    var editingListing by remember { mutableStateOf<Listing?>(null) }

    val filtered = remember(state.myListings, selectedFilter) {
        when (selectedFilter) {
            "live" -> state.myListings.filter { it.isEditableByOwner && it.availabilityStatus != "unavailable" && it.wantedStatus != "fulfilled" }
            "review" -> state.myListings.filter { it.moderationStatus == "live_needs_review" }
            "published" -> state.myListings.filter { it.moderationStatus in setOf("live_reviewed", "published") }
            "unavailable" -> state.myListings.filter { it.availabilityStatus == "unavailable" }
            "fulfilled" -> state.myListings.filter { it.wantedStatus == "fulfilled" }
            else -> state.myListings
        }
    }

    SecondaryPageScaffold(
        title = "My Listings",
        onBack = onBack,
        backButtonTestTag = "my_listings_back_button"
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SubtleRefreshBar(visible = state.myListingsRefreshing)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.sessionExpiredPrompt?.let { prompt ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WarningAmber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth().testTag("my_listings_session_expired_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onRequireSignInAgain,
                                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                                    ) {
                                        Text("Sign in again")
                                    }
                                    TextButton(onClick = onClearSessionExpiredPrompt) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
                state.myListingsOfflineNotice?.let { offlineNotice ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().testTag("my_listings_offline_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = offlineNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onLoadMyListings) {
                                    Text("Retry", color = MarketZedOrange)
                                }
                            }
                        }
                    }
                }
                item {
                    val tabs = listOf(
                        "all" to "All",
                        "live" to "Live",
                        "review" to "Pending Review",
                        "published" to "Reviewed",
                        "unavailable" to "Unavailable",
                        "fulfilled" to "Fulfilled"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tabs) { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (state.myListingsLoading && filtered.isEmpty()) {
                    item {
                        ListRowSkeletonSection(count = 3)
                    }
                } else if (filtered.isEmpty()) {
                    item {
                        EmptyStateView(
                            iconRes = LucideR.drawable.lucide_ic_package,
                            title = if (selectedFilter == "all") "No listings posted yet" else "No listings in this tab",
                            message = if (selectedFilter == "all") {
                                "Publish a product, rental, service, job, or Wanted request to manage it here."
                            } else {
                                "Try switching back to All or publish a new listing."
                            },
                            actionLabel = if (selectedFilter == "all") "Post a Listing" else "Show All Listings",
                            onAction = {
                                if (selectedFilter == "all") onPostListing() else selectedFilter = "all"
                            }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { listing ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenListing(listing.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = listing.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (listing.isPromoted) {
                                            StatusBadgePill(
                                                text = "Sponsored",
                                                backgroundColor = MarketZedOrange,
                                                textColor = Color.White
                                            )
                                        }
                                        StatusBadgePill(
                                            text = when {
                                                listing.isHiddenOrRemoved -> "Hidden/Removed"
                                                listing.wantedStatus == "fulfilled" -> "Fulfilled"
                                                listing.availabilityStatus == "unavailable" -> "Unavailable"
                                                listing.moderationStatus == "live_needs_review" -> "Pending Review"
                                                else -> "Live"
                                            },
                                            backgroundColor = if (listing.isHiddenOrRemoved) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            textColor = if (listing.isHiddenOrRemoved) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            Text(
                                text = PriceFormatter.formatListingPrice(listing.price, listing.priceMode, listing.categoryId) +
                                    " · ${listing.location?.displayFull ?: "Zambia"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!listing.isHiddenOrRemoved) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (listing.isEditableByOwner) {
                                        item {
                                            OutlinedButton(
                                                onClick = { editingListing = listing },
                                                modifier = Modifier.testTag("my_listing_edit_${listing.id}")
                                            ) { Text("Edit") }
                                        }
                                    }
                                    if (listing.availabilityStatus == "unavailable") {
                                        item {
                                            OutlinedButton(onClick = { onManageListing(listing.id, "mark_available") }) {
                                                Text("Mark Available")
                                            }
                                        }
                                    } else if (listing.wantedStatus != "fulfilled") {
                                        item {
                                            OutlinedButton(onClick = { onManageListing(listing.id, "mark_unavailable") }) {
                                                Text("Mark Unavailable")
                                            }
                                        }
                                    }
                                    if (listing.isWanted && listing.wantedStatus != "fulfilled") {
                                        item {
                                            OutlinedButton(onClick = { onManageListing(listing.id, "mark_fulfilled") }) {
                                                Text("Mark Fulfilled")
                                            }
                                        }
                                        item {
                                            OutlinedButton(onClick = { onManageListing(listing.id, "renew_wanted") }) {
                                                Text("Renew 60d")
                                            }
                                        }
                                    }
                                    if (listing.isEditableByOwner && listing.availabilityStatus != "unavailable" && listing.wantedStatus != "fulfilled") {
                                        item {
                                            Button(
                                                onClick = { onPromoteListing(listing) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                                            ) {
                                                Text("Promote")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.myListingsLoading) {
                    item { ListRowSkeletonSection(count = 1) }
                }
            }
        }
        }
    }

    editingListing?.let { target ->
        ListingEditBottomSheet(
            listing = target,
            onDismiss = { editingListing = null },
            onUploadImage = onUploadEditImage,
            onSaveChanges = onReviseListing
        )
    }
}

@Composable
fun PromotionsScreen(
    state: AccountUiState,
    preselectedListing: Listing?,
    onBack: () -> Unit,
    onLoadPromotions: () -> Unit,
    onStartPromotionPayment: (String, Int, String, String, String?) -> Unit,
    onCheckStatus: () -> Unit,
    onClearPaymentState: () -> Unit,
    onClearPromotionsError: () -> Unit,
    onOpenMyListings: () -> Unit = {},
    onOpenPostListing: () -> Unit = {}
) {
    PromoteBusinessScreen(
        state = state,
        preselectedListing = preselectedListing,
        onBack = onBack,
        onLoadPromotions = onLoadPromotions,
        onStartPromotionPayment = onStartPromotionPayment,
        onCheckStatus = onCheckStatus,
        onClearPaymentState = onClearPaymentState,
        onClearPromotionsError = onClearPromotionsError,
        onOpenMyListings = onOpenMyListings,
        onOpenPostListing = onOpenPostListing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onLoadPayments: () -> Unit,
    onClearPaymentsError: () -> Unit,
    onOpenVerification: () -> Unit = {},
    onOpenPromotions: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { onLoadPayments() }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.paymentsError) {
        val err = state.paymentsError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showError(err, actionLabel = "Retry", onAction = onLoadPayments)
        onClearPaymentsError()
    }

    SecondaryPageScaffold(
        title = "Payment History",
        onBack = onBack,
        backButtonTestTag = "payment_history_back_button"
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.paymentsLoading && state.payments.isEmpty()) {
                item { ListRowSkeletonSection(count = 4) }
            } else if (state.payments.isEmpty()) {
                item {
                    val isVerified = state.profile?.hasVerifiedEntitlement == true
                    EmptyStateView(
                        iconRes = LucideR.drawable.lucide_ic_credit_card,
                        title = "No payments yet",
                        message = "Your account verification and promotion Lipila receipts will appear here.",
                        actionLabel = if (!isVerified) "Verify Account" else "Explore Promotions",
                        onAction = {
                            if (!isVerified) onOpenVerification() else onOpenPromotions()
                        }
                    )
                }
            } else {
                items(state.payments) { p ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (p.purpose == "account_verification") "Account Verification" else "Listing Promotion",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = p.amount?.let { PriceFormatter.formatKwachaAmount(it) } ?: p.currency,
                                    fontWeight = FontWeight.Bold,
                                    color = MarketZedOrange
                                )
                            }
                            Text("Status: ${p.status}", style = MaterialTheme.typography.bodySmall)
                            p.referenceId?.let { Text("Ref: $it", style = MaterialTheme.typography.labelSmall) }
                            p.createdAt?.let { Text(DateUtils.formatDateTimeLusaka(DateUtils.parseIsoToMillis(it)), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
                if (state.paymentsLoading) {
                    item { ListRowSkeletonSection(count = 1) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndDiagnosticsScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onSetThemeMode: (String) -> Unit,
    onSetDataSaver: (Boolean) -> Unit,
    onSetWifiOnlyPrefetch: (Boolean) -> Unit,
    onSetGeneralNotifications: (Boolean) -> Unit,
    onSetPromotionalNotifications: (Boolean) -> Unit,
    onSetSmartEngagement: (Boolean) -> Unit,
    onSetMotivationalNotifications: (Boolean) -> Unit,
    onTriggerBackgroundSync: () -> Unit,
    onClearPublicCache: () -> Unit,
    onClearCacheMessage: () -> Unit,
    onSetQuietHours: (Boolean) -> Unit = {},
    onSeedDevFixtures: () -> Unit = {},
    onClearDevFixtures: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val syncStatus = state.backgroundSyncStatus
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.cacheClearedMessage) {
        val msg = state.cacheClearedMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showSuccess(msg)
        onClearCacheMessage()
    }

    SecondaryPageScaffold(
        title = "Settings & Offline Cache",
        onBack = onBack,
        backButtonTestTag = "settings_back_button"
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System").forEach { (key, label) ->
                    FilterChip(
                        selected = state.settings.themeModeName == key,
                        onClick = { onSetThemeMode(key) },
                        label = { Text(label) }
                    )
                }
            }

            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            NotificationPermissionBanner()
            NotificationPreferencesSection(
                prefs = state.notificationPrefs,
                onSetGeneral = onSetGeneralNotifications,
                onSetPromotional = onSetPromotionalNotifications,
                onSetSmartEngagement = onSetSmartEngagement,
                onSetMotivational = onSetMotivationalNotifications,
                onSetQuietHours = onSetQuietHours
            )

            Text("Data & Offline Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Data Saver Mode", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Uses smaller Cloudinary thumbnails & limits background sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.settings.dataSaverEnabled,
                    onCheckedChange = onSetDataSaver
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wi-Fi Only Prefetch", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Skip automatic background feed refreshes on mobile data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.settings.wifiOnlyPrefetch,
                    onCheckedChange = onSetWifiOnlyPrefetch
                )
            }

            Text("Background Sync (WorkManager)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Room ↔ MarketZed API", fontWeight = FontWeight.SemiBold)
                        val badgeColor = when {
                            syncStatus.lastError != null -> WarningAmber
                            syncStatus.statusLabel == "Up to date" -> VerifiedEmerald
                            else -> MarketZedOrange
                        }
                        StatusBadgePill(
                            text = syncStatus.statusLabel,
                            backgroundColor = badgeColor.copy(alpha = 0.16f),
                            textColor = badgeColor
                        )
                    }
                    Text(
                        "Schedule: Every ${syncStatus.syncIntervalHours}h • Network: ${if (syncStatus.wifiOnlyConstraint) "Wi-Fi only" else "Any connected network"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Last sync: ${syncStatus.lastSyncedAt?.let { DateUtils.formatRelativeTime(it) } ?: "Not synced yet"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    syncStatus.lastSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    syncStatus.lastError?.takeIf { it.isNotBlank() }?.let { err ->
                        Text(
                            "Note: $err",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onTriggerBackgroundSync,
                        enabled = !syncStatus.isRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_sync_now_button")
                    ) {
                        if (syncStatus.isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_refresh_cw),
                                contentDescription = "Trigger background sync"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (syncStatus.isRunning) "Syncing in Background..." else "Sync Now in Background")
                    }
                }
            }

            OutlinedButton(
                onClick = onClearPublicCache,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_trash_2),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Public Listing Cache (Keep Drafts)")
            }

            Text("App Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall)
                    Text("API Origin: ${BuildConfig.MARKETZED_API_BASE_URL}", style = MaterialTheme.typography.bodySmall)
                    Text("Offline Store: Room SQLite v4", style = MaterialTheme.typography.bodySmall)
                    Text("WorkManager Sync: ${syncStatus.statusLabel} (Every ${syncStatus.syncIntervalHours}h)", style = MaterialTheme.typography.bodySmall)
                    Text("Ownership Declaration: ${MarketZedCatalogue.OWNERSHIP_DECLARATION_VERSION}", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (BuildConfig.DEBUG && BuildConfig.MARKETZED_TEST_DATA_ENABLED && state.devFixturesAvailable) {
                Text("Developer Test Data (Debug Build Only)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MarketZedOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (state.devFixturesSeeded) {
                                "Active: ${state.devFixturesCount} local Zambian sample listings loaded (35 Products, 20 Rentals, 20 Services, 15 Jobs, 15 Wanted, 15 Swaps)."
                            } else {
                                "Seed 120 local Zambian test listings across all categories to test feeds, filters, editing, and UI density. Excluded automatically from Release APKs."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onSeedDevFixtures,
                                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("dev_seed_fixtures_button")
                            ) {
                                Text(if (state.devFixturesSeeded) "Reload 120 Fixtures" else "Load 120 Test Listings", color = Color.White)
                            }
                            if (state.devFixturesSeeded) {
                                OutlinedButton(
                                    onClick = onClearDevFixtures,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("dev_clear_fixtures_button")
                                ) {
                                    Text("Clear Test Listings")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticInfoScreen(
    destination: AccountDestination,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val (title, body, webPath) = when (destination) {
        AccountDestination.ABOUT -> Triple(
            "About MarketZed",
            "MarketZed is a Zambia-first online marketplace connecting buyers, sellers, property owners, service providers, employers, job seekers, and people posting wanted requests across all 10 provinces.\n\n" +
                "App by Tech Generative Universe (TechGU).\n\n" +
                "Note: MarketZed provides discovery and direct owner contact tools. It does not hold escrow, process item checkouts, or guarantee transactions between users.",
            "/about"
        )
        AccountDestination.SAFETY -> Triple(
            "Safety Guidance",
            "• Meet in busy, well-lit public places when inspecting goods.\n" +
                "• Inspect items carefully before making any payment.\n" +
                "• Avoid sending advance deposits to unfamiliar sellers.\n" +
                "• Never share your mobile-money PIN, bank password, or OTP codes with anyone.\n" +
                "• Contact listing owners directly using their published phone, WhatsApp, or email.\n" +
                "• Use the Report button on any listing that appears fraudulent, counterfeit, or misleading.",
            "/safety"
        )
        AccountDestination.PRIVACY -> Triple(
            "Privacy Policy",
            "MarketZed collects only the account, profile, and listing information needed to operate the marketplace. Private profile contacts, payment history, and reports are owner-scoped and never published publicly unless you choose to include contact preferences on a listing.\n\n" +
                "Saved favorites, recently viewed history, search history, and drafts are stored locally on your device for offline access.",
            "/privacy"
        )
        else -> Triple(
            "Terms & Conditions",
            "By publishing a listing on MarketZed, you confirm lawful ownership or authority to offer the item, property, job, or service, and agree to comply with Zambian law.\n\n" +
                "Normal listing categories require a one-time K30 account activation fee processed server-side via Lipila. Wanted requests are free to publish and expire after 60 days unless renewed.",
            "/terms"
        )
    }

    SecondaryPageScaffold(
        title = title,
        onBack = onBack,
        backButtonTestTag = "static_info_back_button"
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(body, style = MaterialTheme.typography.bodyLarge)

            OutlinedButton(
                onClick = {
                    val url = BuildConfig.MARKETZED_API_BASE_URL.trimEnd('/') + webPath
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_external_link),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Full Web Page")
            }

            if (destination == AccountDestination.ABOUT) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TECHGU_SITE_URL)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_external_link),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Visit Tech Generative Universe (TechGU)")
                }
            }
        }
    }
}
