package com.example.ui.screens.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.CloudinaryUrlHelper
import com.example.core.util.DateUtils
import com.example.core.util.PhoneValidator
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.model.PromotionalBonusRule
import com.example.data.model.SponsoredFormat
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HeroPaymentAmountCard
import com.example.ui.components.LipilaPaymentMethodSummaryCard
import com.example.ui.components.ListingPlaceholderArt
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.PaymentPhaseStatusCard
import com.example.ui.components.PaymentResultDialog
import com.example.ui.components.PaymentSecurityReassuranceBanner
import com.example.ui.components.PaymentUiStatePhase
import com.example.ui.components.PromotionSkeletonSection
import com.example.ui.components.SecondaryPageScaffold
import com.example.ui.components.StatusBadgePill
import com.example.ui.components.rememberShimmerBrush
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald

@Composable
fun PromoteBusinessScreen(
    state: AccountUiState,
    preselectedListing: Listing?,
    bonusRules: List<PromotionalBonusRule> = emptyList(),
    onBack: () -> Unit,
    onLoadPromotions: () -> Unit,
    onStartPromotionPayment: (listingId: String, days: Int, phone: String, payerType: String, campaignFormat: String?) -> Unit,
    onCheckStatus: () -> Unit,
    onClearPaymentState: () -> Unit,
    onClearPromotionsError: () -> Unit,
    onOpenMyListings: () -> Unit = {},
    onOpenPostListing: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { onLoadPromotions() }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.promotionsError) {
        val err = state.promotionsError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showWarning(err, actionLabel = "Retry", onAction = onLoadPromotions)
        onClearPromotionsError()
    }

    // Eligible live listings for sponsorship (strictly excludes Wanted, unavailable, and prohibited land/property-sale)
    val eligibleListings = remember(state.myListings) {
        state.myListings.filter { listing ->
            listing.isEditableByOwner &&
                !listing.isWanted &&
                listing.availabilityStatus != "unavailable" &&
                listing.wantedStatus != "fulfilled" &&
                !MarketZedCatalogue.isProhibitedLandOrPropertySale(listing)
        }
    }

    var selectedListingId by remember(preselectedListing, eligibleListings) {
        mutableStateOf(
            preselectedListing?.id?.takeIf { id -> eligibleListings.any { it.id == id } }
                ?: eligibleListings.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedFormat by remember { mutableStateOf(SponsoredFormat.CAROUSEL_1_3_1) }
    var selectedDays by remember { mutableIntStateOf(7) }
    var phone by remember {
        mutableStateOf(PhoneValidator.autoFixZambianInput(state.profile?.contactNumber.orEmpty()))
    }
    var showConfirmPaymentDialog by remember { mutableStateOf(false) }
    var showPaymentResultDialog by remember { mutableStateOf(false) }

    val selectedListing = eligibleListings.firstOrNull { it.id == selectedListingId }
    val prices = state.promotionPricing?.prices.orEmpty()
    val selectedAmount = prices[selectedDays.toString()]
    val paymentPhase = state.resolvePaymentPhase(
        purposeLabel = "Promote Listing · $selectedDays Days",
        fallbackAmountZmw = selectedAmount ?: 0.0,
        successDetail = "Your sponsored promotion is now scheduled and active."
    )

    LaunchedEffect(state.activePaymentStatus, state.activePaymentId) {
        val status = state.activePaymentStatus?.trim()?.lowercase()
        if (!state.activePaymentId.isNullOrBlank() && status in setOf("successful", "failed", "cancelled", "expired")) {
            showPaymentResultDialog = true
        }
    }

    val approvedBonusRules = remember(bonusRules) {
        bonusRules.filter { it.approvedByPolicy }
    }

    // Real seller analytics strictly derived from recorded backend values (or 0 when unavailable)
    val recordedImpressions = remember(state.promotions, state.myListings) {
        (state.promotions.sumOf { it.impressions ?: 0 } +
            state.myListings.sumOf { it.impressionsCount ?: 0 })
    }
    val recordedClicks = remember(state.promotions, state.myListings) {
        (state.promotions.sumOf { it.clicks ?: 0 } +
            state.myListings.sumOf { it.clicksCount ?: 0 })
    }
    val recordedVisits = remember(state.promotions, state.myListings) {
        (state.promotions.sumOf { it.listingVisits ?: 0 } +
            state.myListings.sumOf { it.visitsCount ?: 0 })
    }

    SecondaryPageScaffold(
        title = "Sponsor & Promote",
        onBack = onBack,
        backButtonTestTag = "promotions_back_button"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("promote_business_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (selectedAmount != null) {
                    HeroPaymentAmountCard(
                        purposeBadge = "SPONSORED PLACEMENT · $selectedDays DAYS",
                        purposeTitle = selectedFormat.label,
                        amountZmw = selectedAmount,
                        shortExplanation = "Promote your listing with priority visibility. Activates only after confirmed Lipila payment."
                    )
                } else {
                    Text(
                        text = "Sponsor a Live Listing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pricing is loaded live from MarketZed. Campaigns activate only after backend-confirmed Lipila payment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Real Seller Analytics Snapshot
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recorded Campaign Performance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnalyticsMetricTile("Impressions", recordedImpressions.toString(), Modifier.weight(1f))
                            AnalyticsMetricTile("Clicks", recordedClicks.toString(), Modifier.weight(1f))
                            AnalyticsMetricTile("Listing visits", recordedVisits.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            if (approvedBonusRules.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VerifiedEmerald.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Approved Promotional Rules", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = VerifiedEmerald)
                            approvedBonusRules.forEach { rule ->
                                Text("${rule.title}: ${rule.description}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (state.promotionsLoading && prices.isEmpty()) {
                item { PromotionSkeletonSection() }
            } else if (prices.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = state.promotionsError ?: "Promotion prices unavailable. Connect to load current ZMW prices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = onLoadPromotions) {
                                Text("Reload live rates", color = MarketZedOrange)
                            }
                        }
                    }
                }
            } else {
                // Step 1: Sponsored Format Selector
                item {
                    Text("1. Choose Placement Format", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SponsoredFormat.entries.forEach { format ->
                            val isSelected = selectedFormat == format
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MarketZedOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MarketZedOrange else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFormat = format }
                                    .testTag("promo_format_${format.wireValue}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(selected = isSelected, onClick = { selectedFormat = format })
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(format.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(format.placementSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 2: Duration & Live Price
                item {
                    Text("2. Choose Sponsorship Duration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(7, "7 Days · Quick Boost", "Priority placement in Sponsored slots for 1 week"),
                            Triple(14, "14 Days · Standard", "Extended visibility across Home and Explore discovery"),
                            Triple(30, "30 Days · Maximum Reach", "Best value for rentals, electronics, vehicles, and services")
                        ).forEach { (days, title, benefit) ->
                            val price = prices[days.toString()]
                            val isSelected = selectedDays == days
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MarketZedOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MarketZedOrange else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDays = days }
                                    .testTag("promo_option_${days}d")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(selected = isSelected, onClick = { selectedDays = days })
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(benefit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = price?.let { PriceFormatter.formatKwachaAmount(it) } ?: "—",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MarketZedOrange
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 3: Select Live Listing
                if (eligibleListings.isEmpty()) {
                    item {
                        EmptyStateView(
                            iconRes = LucideR.drawable.lucide_ic_megaphone,
                            title = "No eligible live listings yet",
                            message = "Publish a product, rental property, or service listing first before sponsoring it.",
                            actionLabel = "Post a Listing",
                            onAction = onOpenPostListing
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = onOpenMyListings) {
                                Text("Go to My Listings", color = MarketZedOrange)
                            }
                        }
                    }
                } else {
                    item {
                        Text("3. Select Eligible Listing", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            eligibleListings.forEach { listingItem ->
                                val isSelected = selectedListingId == listingItem.id
                                val firstImg = listingItem.images.firstOrNull()
                                val thumbUrl = CloudinaryUrlHelper.optimizeForFeed(
                                    url = firstImg?.url,
                                    thumbUrl = firstImg?.thumbUrl,
                                    dataSaverEnabled = state.settings.dataSaverEnabled
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MarketZedOrange.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MarketZedOrange else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedListingId = listingItem.id }
                                        .testTag("promo_select_listing_${listingItem.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(selected = isSelected, onClick = { selectedListingId = listingItem.id })
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            if (!thumbUrl.isNullOrBlank()) {
                                                SubcomposeAsyncImage(
                                                    model = thumbUrl,
                                                    contentDescription = listingItem.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                    loading = { Box(modifier = Modifier.fillMaxSize().background(rememberShimmerBrush())) },
                                                    error = { ListingPlaceholderArt(categoryId = listingItem.categoryId) }
                                                )
                                            } else {
                                                ListingPlaceholderArt(categoryId = listingItem.categoryId)
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = listingItem.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = PriceFormatter.formatListingPrice(listingItem.price, listingItem.priceMode, listingItem.categoryId),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MarketZedOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${MarketZedCatalogue.categoryLabel(listingItem.categoryId)} · ${listingItem.location?.displayTown ?: "Zambia"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Step 4: Mobile Money Summary, Payer Number & Confirm CTA
                    item {
                        LipilaPaymentMethodSummaryCard(
                            instructions = "Review your selected placement and duration, then approve the Lipila Mobile Money prompt on your phone."
                        )
                    }

                    item {
                        val promoPhonePreview = PhoneValidator.normalizeZambianNumber(phone)
                            ?.takeIf { it != phone.trim() }
                        MarketZedOutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Money Number (e.g. 0969331234)") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_smartphone),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            supportingText = promoPhonePreview?.let { fixed -> { Text("Auto-formats to $fixed") } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focus ->
                                    if (!focus.isFocused && phone.isNotBlank()) {
                                        phone = PhoneValidator.autoFixZambianInput(phone)
                                    }
                                }
                                .testTag("promo_phone_input")
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PaymentPhaseStatusCard(
                                phase = paymentPhase,
                                onCheckStatus = if (state.activePaymentId != null && !state.paymentBusy) onCheckStatus else null,
                                onRetry = {
                                    onClearPaymentState()
                                    if (selectedListing != null && selectedAmount != null) {
                                        showConfirmPaymentDialog = true
                                    }
                                },
                                onDone = onClearPaymentState,
                                modifier = Modifier.testTag("promo_payment_status_card")
                            )

                            if (state.activePaymentId == null || paymentPhase is PaymentUiStatePhase.Failed || paymentPhase is PaymentUiStatePhase.Cancelled) {
                                Button(
                                    onClick = {
                                        onClearPaymentState()
                                        val fixedPhone = PhoneValidator.autoFixZambianInput(phone)
                                        phone = fixedPhone
                                        if (PhoneValidator.normalizeZambianNumber(fixedPhone) == null) {
                                            snackbar?.showWarning("Enter a valid Zambian mobile money number first.")
                                        } else {
                                            showConfirmPaymentDialog = true
                                        }
                                    },
                                    enabled = selectedListingId.isNotBlank() && !state.paymentBusy && selectedAmount != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("promo_review_and_pay_button")
                                ) {
                                    Text(
                                        text = selectedAmount?.let {
                                            "Pay ${PriceFormatter.formatKwachaAmount(it)} via Lipila"
                                        } ?: "Review & Pay via Lipila",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            PaymentSecurityReassuranceBanner()
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Your Promotion History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (state.promotions.isEmpty()) {
                item {
                    EmptyStateView(
                        iconRes = LucideR.drawable.lucide_ic_megaphone,
                        title = "No promotions yet",
                        message = "Sponsored campaigns you launch will show impressions, clicks, and expiry status here."
                    )
                }
            } else {
                items(state.promotions) { promo ->
                    val resolvedTitle = promo.listingTitle?.takeIf { it.isNotBlank() }
                        ?: "Listing Promotion (${promo.durationDays} days)"
                    val formatLabel = SponsoredFormat.entries.firstOrNull {
                        it.wireValue.equals(promo.campaignFormat, ignoreCase = true)
                    }?.label
                    val formattedExpiry = promo.expiresAt?.let { raw ->
                        val ms = DateUtils.parseIsoToMillis(raw)
                        if (ms != null && ms > 0L) DateUtils.formatDateTimeLusaka(ms) else raw
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = resolvedTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                StatusBadgePill(
                                    text = promo.status.replaceFirstChar { it.uppercase() },
                                    backgroundColor = if (promo.status.equals("active", ignoreCase = true)) VerifiedEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                    textColor = if (promo.status.equals("active", ignoreCase = true)) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = buildString {
                                    append("${promo.durationDays} days")
                                    if (formatLabel != null) append(" · $formatLabel")
                                    promo.amount?.let { append(" · ${PriceFormatter.formatKwachaAmount(it)}") }
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Impressions: ${promo.impressions ?: 0} · Clicks: ${promo.clicks ?: 0} · Visits: ${promo.listingVisits ?: 0}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            formattedExpiry?.let {
                                Text("Expires: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Mandatory Pre-Payment Confirmation Dialog
    if (showConfirmPaymentDialog && selectedListing != null && selectedAmount != null) {
        AlertDialog(
            onDismissRequest = { showConfirmPaymentDialog = false },
            title = { Text("Confirm Sponsorship Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Please confirm your promotion details before sending the mobile money prompt:")
                    Text("Listing: ${selectedListing.title}", fontWeight = FontWeight.SemiBold)
                    Text("Placement: ${selectedFormat.label}")
                    Text("Duration: $selectedDays days")
                    Text("Amount: ${PriceFormatter.formatKwachaAmount(selectedAmount)}", fontWeight = FontWeight.Bold, color = MarketZedOrange)
                    Text("Payer Number: $phone")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPaymentDialog = false
                        onStartPromotionPayment(selectedListing.id, selectedDays, phone, "self", selectedFormat.wireValue)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                    modifier = Modifier.testTag("promo_confirm_pay_button")
                ) {
                    Text("Send Prompt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPaymentResultDialog && paymentPhase !is PaymentUiStatePhase.Idle && paymentPhase !is PaymentUiStatePhase.Processing) {
        PaymentResultDialog(
            phase = paymentPhase,
            onDismiss = { showPaymentResultDialog = false },
            onCheckStatus = onCheckStatus,
            onRetry = {
                onClearPaymentState()
                if (selectedListing != null && selectedAmount != null) {
                    showConfirmPaymentDialog = true
                }
            },
            onSuccessContinue = { onClearPaymentState() }
        )
    }
}

@Composable
private fun AnalyticsMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
