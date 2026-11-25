package com.example.ui.screens.admin

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.DateUtils
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.model.SponsoredFormat
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.ListRowSkeletonSection
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber

@Composable
internal fun AdminOverviewTab(
    state: AdminUiState,
    onRefresh: () -> Unit,
    onSelectTab: (AdminTab) -> Unit,
    onOpenBroadcastDialog: () -> Unit,
    onOpenPricingDialog: () -> Unit,
    onOpenGrantPromoDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.overviewLoading && state.overview == null) {
            item { ListRowSkeletonSection(count = 4) }
        } else if (state.overviewError != null && state.overview == null) {
            item {
                ErrorStateView(
                    title = "Overview unavailable",
                    message = state.overviewError,
                    onRetry = onRefresh
                )
            }
        } else {
            val ov = state.overview
            if (ov != null) {
                item {
                    Text(
                        text = "Platform Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminMetricCard(
                                label = "Pending Review",
                                value = ov.pendingReviewListings.toString(),
                                iconRes = LucideR.drawable.lucide_ic_circle_alert,
                                accent = WarningAmber,
                                onClick = { onSelectTab(AdminTab.LISTINGS) },
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                label = "Live Listings",
                                value = ov.liveListings.toString(),
                                iconRes = LucideR.drawable.lucide_ic_package,
                                accent = VerifiedEmerald,
                                onClick = { onSelectTab(AdminTab.LISTINGS) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminMetricCard(
                                label = "Active Promos",
                                value = ov.activePromotions.toString(),
                                iconRes = LucideR.drawable.lucide_ic_megaphone,
                                accent = MarketZedOrange,
                                onClick = { onSelectTab(AdminTab.PROMOTIONS) },
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                label = "Open Reports",
                                value = ov.openReports.toString(),
                                iconRes = LucideR.drawable.lucide_ic_flag,
                                accent = MaterialTheme.colorScheme.error,
                                onClick = { onSelectTab(AdminTab.REPORTS) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminMetricCard(
                                label = "Total Users",
                                value = "${ov.totalUsers} (${ov.verifiedUsers} verified)",
                                iconRes = LucideR.drawable.lucide_ic_user,
                                accent = MarketZedOrange,
                                onClick = { onSelectTab(AdminTab.USERS) },
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                label = "Revenue (ZMW)",
                                value = PriceFormatter.formatKwachaAmount(ov.totalRevenueZmw),
                                iconRes = LucideR.drawable.lucide_ic_banknote,
                                accent = VerifiedEmerald,
                                onClick = { onSelectTab(AdminTab.PAYMENTS) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Quick Admin Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminActionShortcutRow(
                    iconRes = LucideR.drawable.lucide_ic_megaphone,
                    title = "Send Platform Broadcast",
                    subtitle = "Push an announcement or notice to MarketZed users",
                    onClick = onOpenBroadcastDialog
                )
                AdminActionShortcutRow(
                    iconRes = LucideR.drawable.lucide_ic_sparkles,
                    title = "Grant Listing Promotion",
                    subtitle = "Activate a 7, 14, or 30-day promo on any live listing",
                    onClick = onOpenGrantPromoDialog
                )
                AdminActionShortcutRow(
                    iconRes = LucideR.drawable.lucide_ic_banknote,
                    title = "Update Promotion Pricing",
                    subtitle = "Adjust 7d, 14d, and 30d ZMW promotion rates",
                    onClick = onOpenPricingDialog
                )
            }
        }

        val logs = state.overview?.recentActions.orEmpty()
        if (logs.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Moderation Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(logs, key = { it.id.ifBlank { "${it.action}_${it.targetId}_${it.createdAt}" } }) { log ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.action.uppercase(), style = MaterialTheme.typography.labelMedium, color = MarketZedOrange, fontWeight = FontWeight.Bold)
                            log.createdAt?.let {
                                Text(DateUtils.formatRelativeTime(DateUtils.parseIsoToMillis(it)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (log.summary.isNotBlank()) {
                            Text(log.summary, style = MaterialTheme.typography.bodySmall)
                        }
                        log.actorEmail?.let {
                            Text("By: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminListingsTab(
    state: AdminUiState,
    onSetFilter: (String) -> Unit,
    onSetQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenListing: (String) -> Unit,
    onRequestModeration: (Listing, String) -> Unit
) {
    val filters = listOf(
        "all" to "All",
        "live_needs_review" to "Needs Review",
        "live_reviewed" to "Approved",
        "promoted" to "Sponsored",
        "hidden" to "Hidden",
        "removed" to "Removed"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MarketZedOutlinedTextField(
                value = state.listingQuery,
                onValueChange = onSetQuery,
                label = { Text("Search listings by title, seller, or ID") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onSearch, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_arrow_right),
                            contentDescription = "Search listings"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_listings_search")
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = state.listingStatusFilter == key,
                        onClick = { onSetFilter(key) },
                        label = { Text(label) }
                    )
                }
            }
        }
        if (state.listingsLoading && state.listings.isEmpty()) {
            item { ListRowSkeletonSection(count = 4) }
        } else if (state.listingsError != null && state.listings.isEmpty()) {
            item {
                ErrorStateView(
                    title = "Could not load moderation queue",
                    message = state.listingsError,
                    onRetry = onSearch
                )
            }
        } else if (state.listings.isEmpty()) {
            item {
                EmptyStateView(
                    iconRes = LucideR.drawable.lucide_ic_package,
                    title = "No listings match this filter",
                    message = "Switch filter tabs or adjust your search query."
                )
            }
        } else {
            items(state.listings, key = { it.id }) { listing ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenListing(listing.id) }
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
                                text = listing.title.ifBlank { listing.id },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            StatusBadgePill(
                                text = listing.moderationStatus ?: "live",
                                backgroundColor = when {
                                    listing.isHiddenOrRemoved -> MaterialTheme.colorScheme.errorContainer
                                    listing.moderationStatus == "live_needs_review" -> WarningAmber.copy(alpha = 0.2f)
                                    else -> VerifiedEmerald.copy(alpha = 0.16f)
                                },
                                textColor = when {
                                    listing.isHiddenOrRemoved -> MaterialTheme.colorScheme.onErrorContainer
                                    listing.moderationStatus == "live_needs_review" -> WarningAmber
                                    else -> VerifiedEmerald
                                }
                            )
                        }
                        Text(
                            text = "${MarketZedCatalogue.categoryLabel(listing.categoryId)} · ${PriceFormatter.formatListingPrice(listing.price, listing.priceMode, listing.categoryId)} · ${listing.location?.displayTown ?: "Zambia"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val isProhibitedPolicy = MarketZedCatalogue.isProhibitedLandOrPropertySale(listing)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listing.sellerName?.takeIf { it.isNotBlank() }?.let { seller ->
                                Text("Seller: $seller", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (listing.isPromoted) {
                                StatusBadgePill(
                                    text = "SPONSORED",
                                    backgroundColor = MarketZedOrange,
                                    textColor = Color.White
                                )
                            }
                            if (isProhibitedPolicy) {
                                StatusBadgePill(
                                    text = "Blocked: Land / sale prohibited",
                                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                                    textColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (listing.moderationStatus != "live_reviewed") {
                                item {
                                    Button(
                                        onClick = { onRequestModeration(listing, "approve") },
                                        enabled = !isProhibitedPolicy,
                                        colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) { Text("Approve") }
                                }
                            }
                            if (listing.isHiddenOrRemoved) {
                                item {
                                    OutlinedButton(
                                        onClick = { onRequestModeration(listing, "restore") },
                                        enabled = !isProhibitedPolicy
                                    ) {
                                        Text("Restore")
                                    }
                                }
                            } else {
                                item {
                                    OutlinedButton(onClick = { onRequestModeration(listing, "hide") }) {
                                        Text("Hide")
                                    }
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { onRequestModeration(listing, if (listing.isPromoted) "unpromote" else "promote") },
                                    enabled = !isProhibitedPolicy || listing.isPromoted
                                ) {
                                    Text(if (listing.isPromoted) "Unsponsor" else "Sponsor")
                                }
                            }
                            if (listing.moderationStatus != "removed") {
                                item {
                                    TextButton(onClick = { onRequestModeration(listing, "reject") }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminPromotionsTab(
    state: AdminUiState,
    onSetFilter: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenPricingDialog: () -> Unit,
    onOpenGrantDialog: () -> Unit,
    onManagePromotion: (promotionId: String, action: String, durationDays: Int?, reason: String?, campaignFormat: String?) -> Unit,
    onOpenListing: (String) -> Unit
) {
    val filters = listOf("all" to "All", "active" to "Active", "pending" to "Pending", "expired" to "Expired", "cancelled" to "Cancelled")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Pricing & Grant controls
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
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
                        Text("Live Promotion Rates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onOpenPricingDialog) { Text("Edit Rates", color = MarketZedOrange) }
                    }
                    val prices = state.promotionPricing?.prices.orEmpty()
                    if (prices.isEmpty()) {
                        Text("Current ZMW rates unavailable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("7", "14", "30").forEach { d ->
                                val amount = prices[d]
                                StatusBadgePill(
                                    text = "${d}d: ${amount?.let { PriceFormatter.formatKwachaAmount(it) } ?: "—"}",
                                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                    textColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onOpenGrantDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                    ) {
                        Icon(painter = painterResource(LucideR.drawable.lucide_ic_sparkles), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Promotion to Listing")
                    }
                }
            }
        }

        if (state.bonusRules.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Configured Promotional Rules",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        state.bonusRules.forEach { rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Text(rule.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadgePill(
                                    text = if (rule.approvedByPolicy) "APPROVED" else "POLICY GATED",
                                    backgroundColor = if (rule.approvedByPolicy) VerifiedEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                    textColor = if (rule.approvedByPolicy) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = state.promotionStatusFilter == key,
                        onClick = { onSetFilter(key) },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (state.promotionsLoading && state.promotions.isEmpty()) {
            item { ListRowSkeletonSection(count = 3) }
        } else if (state.promotionsError != null && state.promotions.isEmpty()) {
            item {
                ErrorStateView(
                    title = "Promotions unavailable",
                    message = state.promotionsError,
                    onRetry = onRetry
                )
            }
        } else if (state.promotions.isEmpty()) {
            item {
                EmptyStateView(
                    iconRes = LucideR.drawable.lucide_ic_megaphone,
                    title = "No promotions found",
                    message = "Active and historical promotions appear here."
                )
            }
        } else {
            items(state.promotions, key = { it.id.ifBlank { "${it.listingId}_${it.startsAt}" } }) { promo ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().clickable { if (promo.listingId.isNotBlank()) onOpenListing(promo.listingId) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = promo.listingTitle ?: promo.listingId,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadgePill(
                                text = promo.status.uppercase().ifBlank { "ACTIVE" },
                                backgroundColor = if (promo.status.equals("active", true)) VerifiedEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                textColor = if (promo.status.equals("active", true)) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${promo.durationDays} days" + (promo.amount?.let { " · ${PriceFormatter.formatKwachaAmount(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                            val formatLabel = promo.campaignFormat?.takeIf { it.isNotBlank() }?.let { rawFmt ->
                                SponsoredFormat.entries.firstOrNull { it.wireValue == rawFmt }?.label ?: rawFmt
                            }
                            if (!formatLabel.isNullOrBlank()) {
                                StatusBadgePill(
                                    text = formatLabel,
                                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                    textColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(
                            text = "Impressions: ${promo.impressions ?: 0} · Clicks: ${promo.clicks ?: 0} · Visits: ${promo.visits ?: 0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        promo.userEmail?.let { Text("User: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        promo.expiresAt?.let { Text("Expires: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!promo.status.equals("active", true)) {
                                Button(
                                    onClick = { onManagePromotion(promo.id, "activate", promo.durationDays, null, promo.campaignFormat) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald)
                                ) { Text("Activate") }
                            } else {
                                OutlinedButton(onClick = { onManagePromotion(promo.id, "extend", 7, "Admin 7d extension", promo.campaignFormat) }) {
                                    Text("Extend +7d")
                                }
                                TextButton(onClick = { onManagePromotion(promo.id, "cancel", null, "Cancelled by admin", promo.campaignFormat) }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminReportsTab(
    state: AdminUiState,
    onSetFilter: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenListing: (String) -> Unit,
    onRequestResolve: (com.example.data.model.AdminReportRecord, String, String) -> Unit
) {
    val filters = listOf("open" to "Open", "resolved" to "Resolved", "dismissed" to "Dismissed", "all" to "All")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = state.reportStatusFilter == key,
                        onClick = { onSetFilter(key) },
                        label = { Text(label) }
                    )
                }
            }
        }
        if (state.reportsLoading && state.reports.isEmpty()) {
            item { ListRowSkeletonSection(count = 3) }
        } else if (state.reportsError != null && state.reports.isEmpty()) {
            item {
                ErrorStateView(
                    title = "Reports queue unavailable",
                    message = state.reportsError,
                    onRetry = onRetry
                )
            }
        } else if (state.reports.isEmpty()) {
            item {
                EmptyStateView(
                    iconRes = LucideR.drawable.lucide_ic_shield_check,
                    title = "No reports in this queue",
                    message = "Reported listings from MarketZed users appear here."
                )
            }
        } else {
            items(state.reports, key = { it.id }) { report ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = report.listingTitle ?: report.listingId,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadgePill(
                                text = report.status.uppercase(),
                                backgroundColor = if (report.status.equals("open", true)) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                textColor = if (report.status.equals("open", true)) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("Reason: ${report.reason}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MarketZedOrange)
                        report.details?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        report.reporterEmail?.let {
                            Text("Reported by: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (report.listingId.isNotBlank()) {
                                item {
                                    OutlinedButton(onClick = { onOpenListing(report.listingId) }) { Text("View Listing") }
                                }
                            }
                            if (report.status.equals("open", true)) {
                                item {
                                    Button(
                                        onClick = { onRequestResolve(report, "resolve_hide", "Hide Listing & Resolve Report") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Hide Listing") }
                                }
                                item {
                                    OutlinedButton(onClick = { onRequestResolve(report, "dismiss", "Dismiss Report") }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminUsersTab(
    state: AdminUiState,
    onSetFilter: (String) -> Unit,
    onSetQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onManageUser: (uid: String, action: String, reason: String?) -> Unit
) {
    val filters = listOf("all" to "All", "verified" to "Verified", "activated" to "Activated", "suspended" to "Suspended")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MarketZedOutlinedTextField(
                value = state.userQuery,
                onValueChange = onSetQuery,
                label = { Text("Search users by name, email, or phone") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onSearch, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(painter = painterResource(LucideR.drawable.lucide_ic_arrow_right), contentDescription = "Search users")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_users_search")
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = state.userFilter == key,
                        onClick = { onSetFilter(key) },
                        label = { Text(label) }
                    )
                }
            }
        }
        if (state.usersLoading && state.users.isEmpty()) {
            item { ListRowSkeletonSection(count = 4) }
        } else if (state.usersError != null && state.users.isEmpty()) {
            item {
                ErrorStateView(
                    title = "Users directory unavailable",
                    message = state.usersError,
                    onRetry = onSearch
                )
            }
        } else if (state.users.isEmpty()) {
            item {
                EmptyStateView(
                    iconRes = LucideR.drawable.lucide_ic_user,
                    title = "No matching users",
                    message = "Search by name or email to manage verification and posting access."
                )
            }
        } else {
            items(state.users, key = { it.uid.ifBlank { it.email } }) { user ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (user.verified) {
                                StatusBadgePill(text = "VERIFIED", backgroundColor = VerifiedEmerald.copy(alpha = 0.16f), textColor = VerifiedEmerald)
                            }
                        }
                        Text(
                            text = "${user.accountType.replaceFirstChar { it.uppercase() }} · ${user.town ?: user.province ?: "Zambia"} · ${user.listingsCount} listings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                OutlinedButton(onClick = { onManageUser(user.uid, if (user.verified) "unverify" else "verify", null) }) {
                                    Text(if (user.verified) "Remove Verification" else "Verify Seller")
                                }
                            }
                            item {
                                OutlinedButton(onClick = { onManageUser(user.uid, if (user.isPostingActivated) "disable_posting" else "enable_posting", null) }) {
                                    Text(if (user.isPostingActivated) "Disable Posting" else "Enable Posting")
                                }
                            }
                            item {
                                TextButton(onClick = { onManageUser(user.uid, if (user.suspended) "unsuspend" else "suspend", null) }) {
                                    Text(
                                        text = if (user.suspended) "Reinstate" else "Suspend",
                                        color = if (user.suspended) VerifiedEmerald else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminPaymentsTab(
    state: AdminUiState,
    onSetFilter: (String) -> Unit,
    onRetry: () -> Unit
) {
    val filters = listOf("all" to "All", "successful" to "Successful", "pending" to "Pending", "failed" to "Failed")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = state.paymentStatusFilter == key,
                        onClick = { onSetFilter(key) },
                        label = { Text(label) }
                    )
                }
            }
        }
        if (state.paymentsLoading && state.payments.isEmpty()) {
            item { ListRowSkeletonSection(count = 4) }
        } else if (state.paymentsError != null && state.payments.isEmpty()) {
            item {
                ErrorStateView(
                    title = "Payments ledger unavailable",
                    message = state.paymentsError,
                    onRetry = onRetry
                )
            }
        } else if (state.payments.isEmpty()) {
            item {
                EmptyStateView(
                    iconRes = LucideR.drawable.lucide_ic_credit_card,
                    title = "No payments in this filter",
                    message = "Lipila K30 activations and promotion payments appear here."
                )
            }
        } else {
            items(state.payments, key = { it.id.ifBlank { "${it.referenceId}_${it.createdAt}" } }) { p ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (p.purpose == "account_verification") "K30 Activation" else "Listing Promotion",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = p.amount?.let { PriceFormatter.formatKwachaAmount(it) } ?: p.currency,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MarketZedOrange
                            )
                        }
                        p.userEmail?.let { Text("User: $it", style = MaterialTheme.typography.bodySmall) }
                        Text("Status: ${p.status.uppercase()}", style = MaterialTheme.typography.labelSmall, color = if (p.status.equals("successful", true)) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant)
                        p.referenceId?.let { Text("Ref: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        p.createdAt?.let { Text(DateUtils.formatDateTimeLusaka(DateUtils.parseIsoToMillis(it)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    label: String,
    value: String,
    @DrawableRes iconRes: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminActionShortcutRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MarketZedOrange,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
