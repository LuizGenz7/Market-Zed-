package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.data.model.AdminReportRecord
import com.example.data.model.Listing
import com.example.data.model.SponsoredFormat
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.SecondaryPageScaffold
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald

internal data class PendingListingAction(
    val listing: Listing,
    val action: String,
    val title: String,
    val confirmLabel: String,
    val isDestructive: Boolean
)

internal data class PendingReportResolution(
    val report: AdminReportRecord,
    val action: String,
    val label: String
)

@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    adminEmail: String,
    adminRoleLabel: String?,
    state: AdminUiState,
    onBack: () -> Unit,
    onLoadInitial: () -> Unit,
    onSelectTab: (AdminTab) -> Unit,
    onRefreshTab: () -> Unit,
    onSetListingFilter: (String) -> Unit,
    onSetListingQuery: (String) -> Unit,
    onSearchListings: () -> Unit,
    onModerateListing: (listingId: String, action: String, reason: String?) -> Unit,
    onOpenListing: (String) -> Unit,
    onSetPromotionFilter: (String) -> Unit,
    onManagePromotion: (promotionId: String, action: String, durationDays: Int?, reason: String?, campaignFormat: String?) -> Unit,
    onGrantPromotion: (listingId: String, durationDays: Int, note: String?, campaignFormat: String?) -> Unit,
    onUpdatePromotionPricing: (Double?, Double?, Double?) -> Unit,
    onSetReportFilter: (String) -> Unit,
    onResolveReport: (reportId: String, action: String, note: String?) -> Unit,
    onSetUserFilter: (String) -> Unit,
    onSetUserQuery: (String) -> Unit,
    onSearchUsers: () -> Unit,
    onManageUser: (uid: String, action: String, reason: String?) -> Unit,
    onSetPaymentFilter: (String) -> Unit,
    onSendBroadcast: (title: String, message: String, categoryId: String?, listingId: String?) -> Unit,
    onClearFeedback: () -> Unit
) {
    BackHandler(onBack = onBack)

    if (!isAdmin) {
        SecondaryPageScaffold(
            title = "Admin Dashboard",
            onBack = onBack,
            backButtonTestTag = "admin_back_button"
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .testTag("admin_access_denied"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_lock),
                                contentDescription = "Access locked",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Text(
                        text = "Admin Access Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "This control center is restricted to verified MarketZed administrators.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Back to Account")
                    }
                }
            }
        }
        return
    }

    val snackbar = LocalTopSnackbarController.current
    LaunchedEffect(isAdmin) {
        if (isAdmin) onLoadInitial()
    }
    LaunchedEffect(state.actionSuccess) {
        val msg = state.actionSuccess?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showSuccess(msg)
        onClearFeedback()
    }
    LaunchedEffect(state.actionError) {
        val err = state.actionError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showError(err)
        onClearFeedback()
    }

    var pendingListingAction by remember { mutableStateOf<PendingListingAction?>(null) }
    var pendingReportAction by remember { mutableStateOf<PendingReportResolution?>(null) }
    var showPricingDialog by remember { mutableStateOf(false) }
    var showGrantPromoDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    SecondaryPageScaffold(
        title = "Admin Dashboard",
        onBack = onBack,
        backButtonTestTag = "admin_back_button",
        actions = {
            IconButton(
                onClick = onRefreshTab,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("admin_refresh_button")
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_refresh_cw),
                    contentDescription = "Refresh admin view"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.actionBusy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MarketZedOrange
                )
            }

            // Admin Verified Header Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    VerifiedEmerald.copy(alpha = 0.12f),
                                    MarketZedOrange.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                        contentDescription = "Verified administrator",
                        tint = VerifiedEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = adminEmail.ifBlank { "Administrator Session" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Role: ${(adminRoleLabel ?: "admin").uppercase()} · Moderation & Promotions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadgePill(
                        text = "ADMIN",
                        backgroundColor = VerifiedEmerald,
                        textColor = Color.White
                    )
                }
            }

            // Scrollable Admin Section Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AdminTab.entries) { tab ->
                    val selected = state.selectedTab == tab
                    val badgeCount = when (tab) {
                        AdminTab.LISTINGS -> state.overview?.pendingReviewListings ?: 0
                        AdminTab.REPORTS -> state.overview?.openReports ?: 0
                        AdminTab.PROMOTIONS -> state.overview?.pendingPromotions ?: 0
                        else -> 0
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        label = {
                            Text(
                                text = if (badgeCount > 0) "${tab.label} ($badgeCount)" else tab.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MarketZedOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (state.selectedTab) {
                AdminTab.OVERVIEW -> AdminOverviewTab(
                    state = state,
                    onRefresh = onRefreshTab,
                    onSelectTab = onSelectTab,
                    onOpenBroadcastDialog = { showBroadcastDialog = true },
                    onOpenPricingDialog = { showPricingDialog = true },
                    onOpenGrantPromoDialog = { showGrantPromoDialog = true }
                )
                AdminTab.LISTINGS -> AdminListingsTab(
                    state = state,
                    onSetFilter = onSetListingFilter,
                    onSetQuery = onSetListingQuery,
                    onSearch = onSearchListings,
                    onOpenListing = onOpenListing,
                    onRequestModeration = { listing, action ->
                        pendingListingAction = when (action) {
                            "approve" -> PendingListingAction(listing, "approve", "Approve Listing", "Approve", false)
                            "hide" -> PendingListingAction(listing, "hide", "Hide Listing", "Hide", true)
                            "restore" -> PendingListingAction(listing, "restore", "Restore Listing", "Restore", false)
                            "reject" -> PendingListingAction(listing, "reject", "Remove / Reject Listing", "Remove", true)
                            "promote" -> PendingListingAction(listing, "promote", "Sponsor Listing", "Sponsor", false)
                            else -> PendingListingAction(listing, "unpromote", "Remove Sponsored Flag", "Remove Flag", false)
                        }
                    }
                )
                AdminTab.PROMOTIONS -> AdminPromotionsTab(
                    state = state,
                    onSetFilter = onSetPromotionFilter,
                    onRetry = onRefreshTab,
                    onOpenPricingDialog = { showPricingDialog = true },
                    onOpenGrantDialog = { showGrantPromoDialog = true },
                    onManagePromotion = onManagePromotion,
                    onOpenListing = onOpenListing
                )
                AdminTab.REPORTS -> AdminReportsTab(
                    state = state,
                    onSetFilter = onSetReportFilter,
                    onRetry = onRefreshTab,
                    onOpenListing = onOpenListing,
                    onRequestResolve = { report, action, label ->
                        pendingReportAction = PendingReportResolution(report, action, label)
                    }
                )
                AdminTab.USERS -> AdminUsersTab(
                    state = state,
                    onSetFilter = onSetUserFilter,
                    onSetQuery = onSetUserQuery,
                    onSearch = onSearchUsers,
                    onManageUser = onManageUser
                )
                AdminTab.PAYMENTS -> AdminPaymentsTab(
                    state = state,
                    onSetFilter = onSetPaymentFilter,
                    onRetry = onRefreshTab
                )
            }
        }
    }

    // Listing Moderation Dialog
    pendingListingAction?.let { pending ->
        var reason by remember(pending) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingListingAction = null },
            title = { Text(pending.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = pending.listing.title.ifBlank { pending.listing.id },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    MarketZedOutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Moderation note / reason (optional)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_message_square_text),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModerateListing(pending.listing.id, pending.action, reason)
                        pendingListingAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pending.isDestructive) MaterialTheme.colorScheme.error else MarketZedOrange
                    )
                ) {
                    Text(pending.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingListingAction = null }) { Text("Cancel") }
            }
        )
    }

    // Report Resolution Dialog
    pendingReportAction?.let { pending ->
        var note by remember(pending) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingReportAction = null },
            title = { Text(pending.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Listing: ${pending.report.listingTitle ?: pending.report.listingId}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    MarketZedOutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Resolution note (optional)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_file_text),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveReport(pending.report.id, pending.action, note)
                        pendingReportAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReportAction = null }) { Text("Cancel") }
            }
        )
    }

    // Update Promotion Pricing Dialog
    if (showPricingDialog) {
        val currentPrices = state.promotionPricing?.prices.orEmpty()
        var p7 by remember(currentPrices) { mutableStateOf(currentPrices["7"]?.toString().orEmpty()) }
        var p14 by remember(currentPrices) { mutableStateOf(currentPrices["14"]?.toString().orEmpty()) }
        var p30 by remember(currentPrices) { mutableStateOf(currentPrices["30"]?.toString().orEmpty()) }

        AlertDialog(
            onDismissRequest = { showPricingDialog = false },
            title = { Text("Update Promotion Pricing (ZMW)") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarketZedOutlinedTextField(
                        value = p7,
                        onValueChange = { p7 = it },
                        label = { Text("7-Day Price (ZMW)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_banknote),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MarketZedOutlinedTextField(
                        value = p14,
                        onValueChange = { p14 = it },
                        label = { Text("14-Day Price (ZMW)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_banknote),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MarketZedOutlinedTextField(
                        value = p30,
                        onValueChange = { p30 = it },
                        label = { Text("30-Day Price (ZMW)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_banknote),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePromotionPricing(
                            p7.trim().toDoubleOrNull(),
                            p14.trim().toDoubleOrNull(),
                            p30.trim().toDoubleOrNull()
                        )
                        showPricingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                ) {
                    Text("Save Rates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPricingDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Grant Promotion Dialog
    if (showGrantPromoDialog) {
        var listingId by remember { mutableStateOf("") }
        var durationDays by remember { mutableIntStateOf(7) }
        var campaignFormat by remember { mutableStateOf(SponsoredFormat.CAROUSEL_1_3_1.wireValue) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGrantPromoDialog = false },
            title = { Text("Grant Listing Promotion") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarketZedOutlinedTextField(
                        value = listingId,
                        onValueChange = { listingId = it },
                        label = { Text("Listing ID") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_tag),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Duration", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 14, 30).forEach { days ->
                            FilterChip(
                                selected = durationDays == days,
                                onClick = { durationDays = days },
                                label = { Text("${days}d") }
                            )
                        }
                    }
                    Text("Campaign Format", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SponsoredFormat.entries) { fmt ->
                            FilterChip(
                                selected = campaignFormat == fmt.wireValue,
                                onClick = { campaignFormat = fmt.wireValue },
                                label = { Text(fmt.label) }
                            )
                        }
                    }
                    MarketZedOutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Admin note (optional)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_message_square_text),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGrantPromotion(listingId, durationDays, note, campaignFormat)
                        showGrantPromoDialog = false
                    },
                    enabled = listingId.isNotBlank() && !state.actionBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                ) {
                    if (state.actionBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Activate")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGrantPromoDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Broadcast Announcement Dialog
    if (showBroadcastDialog) {
        var title by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var categoryId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Send Admin Broadcast") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarketZedOutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Announcement Title") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_megaphone),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MarketZedOutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Announcement Message") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_message_square_text),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MarketZedOutlinedTextField(
                        value = categoryId,
                        onValueChange = { categoryId = it },
                        label = { Text("Target Category ID (optional)") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_layers),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendBroadcast(title, message, categoryId, null)
                        showBroadcastDialog = false
                    },
                    enabled = title.isNotBlank() && message.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                ) {
                    Text("Send Broadcast")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) { Text("Cancel") }
            }
        )
    }
}
