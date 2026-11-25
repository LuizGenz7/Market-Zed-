package com.example.ui.screens.account

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.DateUtils
import com.example.data.model.AppNotification
import com.example.data.notifications.LocalAnnouncement
import com.example.data.notifications.MarketZedNotificationHelper
import com.example.data.notifications.NotificationPreferences
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ListRowSkeletonSection
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.SecondaryPageScaffold
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WantedBlue
import com.example.ui.theme.WarningAmber

data class NotificationSemanticStyle(
    @DrawableRes val iconRes: Int,
    val tint: Color,
    val defaultTitle: String,
    val categoryBadge: String
)

object NotificationSemantics {
    fun resolveStyle(type: String, title: String? = null, message: String? = null): NotificationSemanticStyle {
        val raw = "${type.lowercase()} ${title.orEmpty().lowercase()} ${message.orEmpty().lowercase()}"
        return when {
            raw.contains("wanted") -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_handshake,
                tint = WantedBlue,
                defaultTitle = "Wanted Request Update",
                categoryBadge = "Wanted"
            )
            raw.contains("verif") || raw.contains("activation") -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_shield_check,
                tint = VerifiedEmerald,
                defaultTitle = "Account Verification",
                categoryBadge = "Verification"
            )
            raw.contains("payment") || raw.contains("lipila") || raw.contains("receipt") -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_wallet,
                tint = MarketZedOrange,
                defaultTitle = "Payment Update",
                categoryBadge = "Payment"
            )
            raw.contains("promo") || raw.contains("sponsor") || raw.contains("campaign") -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_megaphone,
                tint = MarketZedOrange,
                defaultTitle = "Promotion Update",
                categoryBadge = "Promotion"
            )
            raw.contains("listing") || raw.contains("review") || raw.contains("publish") -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_package,
                tint = MarketZedOrange,
                defaultTitle = "Listing Update",
                categoryBadge = "Listing"
            )
            raw.contains("security") || raw.contains("password") || raw.contains("session") -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_shield,
                tint = WarningAmber,
                defaultTitle = "Security Alert",
                categoryBadge = "Security"
            )
            raw.contains("account") || raw.contains("profile") -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_user,
                tint = WantedBlue,
                defaultTitle = "Account Notice",
                categoryBadge = "Account"
            )
            else -> NotificationSemanticStyle(
                iconRes = LucideR.drawable.lucide_ic_bell,
                tint = MarketZedOrange,
                defaultTitle = "MarketZed Update",
                categoryBadge = "General"
            )
        }
    }
}

@Composable
fun NotificationsScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onMarkAnnouncementRead: (String) -> Unit,
    onOpenListing: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenVerification: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenPromotions: () -> Unit,
    onOpenWanted: () -> Unit,
    onBrowseMarketplace: () -> Unit,
    onSetGeneralNotifications: (Boolean) -> Unit,
    onSetPromotionalNotifications: (Boolean) -> Unit,
    onSetSmartEngagement: (Boolean) -> Unit,
    onSetMotivationalNotifications: (Boolean) -> Unit,
    onClearNotificationsError: () -> Unit,
    onSetQuietHours: (Boolean) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { onRefresh() }
    val snackbar = LocalTopSnackbarController.current
    var showPrefsPanel by remember { mutableStateOf(false) }
    var selectedAnnouncement by remember { mutableStateOf<LocalAnnouncement?>(null) }

    LaunchedEffect(state.notificationsError) {
        val err = state.notificationsError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showError(err, actionLabel = "Retry", onAction = onRefresh)
        onClearNotificationsError()
    }

    SecondaryPageScaffold(
        title = "Notifications",
        onBack = onBack,
        actions = {
            if (state.unreadNotificationCount > 0) {
                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.testTag("notifications_mark_all_read")
                ) {
                    Text(
                        text = "Mark all read",
                        color = MarketZedOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            IconButton(
                onClick = { showPrefsPanel = !showPrefsPanel },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("notifications_preferences_toggle")
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_sliders_horizontal),
                    contentDescription = "Notification preferences",
                    tint = if (showPrefsPanel) MarketZedOrange else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        backButtonTestTag = "notifications_back_button"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("notifications_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NotificationPermissionBanner()
            }

            if (showPrefsPanel) {
                item {
                    NotificationPreferencesSection(
                        prefs = state.notificationPrefs,
                        onSetGeneral = onSetGeneralNotifications,
                        onSetPromotional = onSetPromotionalNotifications,
                        onSetSmartEngagement = onSetSmartEngagement,
                        onSetMotivational = onSetMotivationalNotifications,
                        onSetQuietHours = onSetQuietHours
                    )
                }
            }

            if (state.localAnnouncements.isNotEmpty()) {
                item {
                    Text(
                        text = "Announcements & Highlights",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.localAnnouncements, key = { "ann_${it.id}" }) { ann ->
                    val style = NotificationSemantics.resolveStyle(
                        type = ann.type,
                        title = ann.title,
                        message = ann.message
                    )
                    NotificationStructuredCard(
                        iconRes = style.iconRes,
                        accentTint = style.tint,
                        categoryBadge = style.categoryBadge,
                        title = ann.title,
                        description = ann.message,
                        relativeTime = DateUtils.formatRelativeTime(ann.receivedAtMillis),
                        isRead = ann.read,
                        onClick = {
                            if (!ann.read) onMarkAnnouncementRead(ann.id)
                            when {
                                !ann.listingId.isNullOrBlank() -> onOpenListing(ann.listingId)
                                !ann.categoryId.isNullOrBlank() -> onOpenCategory(ann.categoryId)
                                else -> selectedAnnouncement = ann
                            }
                        },
                        onMarkRead = if (!ann.read) ({ onMarkAnnouncementRead(ann.id) }) else null,
                        onDelete = { onDeleteNotification("ann_${ann.id}") },
                        testTag = "notification_card_ann_${ann.id}"
                    )
                }
            }

            if (state.notificationsLoading && state.notifications.isEmpty()) {
                item { ListRowSkeletonSection(count = 4) }
            } else if (state.notifications.isEmpty() && state.localAnnouncements.isEmpty()) {
                item {
                    EmptyStateView(
                        iconRes = LucideR.drawable.lucide_ic_bell,
                        title = "No notifications yet",
                        message = "Listing updates, verification progress, payments, and promotions appear here.",
                        actionLabel = "Browse Marketplace",
                        onAction = onBrowseMarketplace
                    )
                }
            } else if (state.notifications.isNotEmpty()) {
                if (state.localAnnouncements.isNotEmpty()) {
                    item {
                        Text(
                            text = "Account Activity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(state.notifications, key = { it.id }) { notif ->
                    val style = NotificationSemantics.resolveStyle(
                        type = notif.type,
                        title = notif.title,
                        message = notif.message
                    )
                    NotificationStructuredCard(
                        iconRes = style.iconRes,
                        accentTint = style.tint,
                        categoryBadge = style.categoryBadge,
                        title = notif.title?.takeIf { it.isNotBlank() } ?: style.defaultTitle,
                        description = notif.message,
                        relativeTime = DateUtils.formatRelativeTime(DateUtils.parseIsoToMillis(notif.createdAt)),
                        isRead = notif.read,
                        onClick = {
                            if (!notif.read) onMarkRead(notif.id)
                            navigateFromNotification(
                                notif = notif,
                                onOpenListing = onOpenListing,
                                onOpenVerification = onOpenVerification,
                                onOpenPayments = onOpenPayments,
                                onOpenPromotions = onOpenPromotions,
                                onOpenWanted = onOpenWanted,
                                onOpenCategory = onOpenCategory
                            )
                        },
                        onMarkRead = if (!notif.read) ({ onMarkRead(notif.id) }) else null,
                        onDelete = { onDeleteNotification(notif.id) },
                        testTag = "notification_card_${notif.id}"
                    )
                }
                if (state.notificationsLoading) {
                    item { ListRowSkeletonSection(count = 1) }
                }
            }
        }
    }

    selectedAnnouncement?.let { ann ->
        AlertDialog(
            onDismissRequest = { selectedAnnouncement = null },
            title = { Text(ann.title, fontWeight = FontWeight.Bold) },
            text = { Text(ann.message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { selectedAnnouncement = null }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun navigateFromNotification(
    notif: AppNotification,
    onOpenListing: (String) -> Unit,
    onOpenVerification: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenPromotions: () -> Unit,
    onOpenWanted: () -> Unit,
    onOpenCategory: (String) -> Unit
) {
    val raw = "${notif.type.lowercase()} ${notif.title.orEmpty().lowercase()}"
    when {
        !notif.listingId.isNullOrBlank() -> onOpenListing(notif.listingId)
        raw.contains("verif") || raw.contains("activation") -> onOpenVerification()
        !notif.paymentId.isNullOrBlank() || raw.contains("payment") -> onOpenPayments()
        raw.contains("promo") || raw.contains("sponsor") -> onOpenPromotions()
        raw.contains("wanted") -> onOpenWanted()
        !notif.categoryId.isNullOrBlank() -> onOpenCategory(notif.categoryId)
    }
}

@Composable
private fun NotificationStructuredCard(
    @DrawableRes iconRes: Int,
    accentTint: Color,
    categoryBadge: String,
    title: String,
    description: String,
    relativeTime: String,
    isRead: Boolean,
    onClick: () -> Unit,
    onMarkRead: (() -> Unit)?,
    onDelete: () -> Unit,
    testTag: String
) {
    val containerColor = if (isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        accentTint.copy(alpha = 0.08f)
    }
    val borderColor = if (isRead) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    } else {
        accentTint.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(if (isRead) 1.dp else 1.5.dp, borderColor),
        tonalElevation = if (isRead) 1.dp else 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentTint.copy(alpha = if (isRead) 0.12f else 0.18f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = categoryBadge,
                        tint = accentTint,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accentTint.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = categoryBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentTint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (!isRead) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MarketZedOrange.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MarketZedOrange,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMarkRead != null) {
                        TextButton(
                            onClick = onMarkRead,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_check),
                                contentDescription = null,
                                tint = MarketZedOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark read", color = MarketZedOrange, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("${testTag}_delete")
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_trash_2),
                            contentDescription = "Delete notification",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotificationPermissionBanner() {
    val context = LocalContext.current
    var canPost by remember { mutableStateOf(MarketZedNotificationHelper.canPostSystemNotifications(context)) }
    val snackbar = LocalTopSnackbarController.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        canPost = granted && MarketZedNotificationHelper.canPostSystemNotifications(context)
        if (canPost) {
            snackbar?.showSuccess("Push notifications enabled")
        } else {
            snackbar?.showInfo("You can enable notifications anytime in Android settings.")
        }
    }

    if (!canPost) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MarketZedOrange.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, MarketZedOrange.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_bell_ring),
                    contentDescription = null,
                    tint = MarketZedOrange
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Turn on push notifications",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Get listing updates, payment confirmations, and important announcements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    }
                ) {
                    Text("Enable", color = MarketZedOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun NotificationPreferencesSection(
    prefs: NotificationPreferences,
    onSetGeneral: (Boolean) -> Unit,
    onSetPromotional: (Boolean) -> Unit,
    onSetSmartEngagement: (Boolean) -> Unit,
    onSetMotivational: (Boolean) -> Unit,
    onSetQuietHours: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Notification Preferences",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            PreferenceToggleRow(
                title = "General & Announcements",
                subtitle = "Platform notices, important updates, and listing activity.",
                checked = prefs.generalEnabled,
                onCheckedChange = onSetGeneral,
                testTag = "pref_general_notifications"
            )
            PreferenceToggleRow(
                title = "Promotions & Sponsored",
                subtitle = "Promotions, new arrivals, and sponsored listings across Zambia.",
                checked = prefs.promotionalEnabled,
                onCheckedChange = onSetPromotional,
                testTag = "pref_promotional_notifications"
            )
            PreferenceToggleRow(
                title = "Smart Discovery Reminders",
                subtitle = "Occasional frequency-capped reminders for relevant opportunities.",
                checked = prefs.smartEngagementEnabled && prefs.promotionalEnabled,
                enabled = prefs.promotionalEnabled,
                onCheckedChange = onSetSmartEngagement,
                testTag = "pref_smart_engagement"
            )
            PreferenceToggleRow(
                title = "Encouraging Discovery Alerts",
                subtitle = "Scheduled motivational prompts from MarketZed.",
                checked = prefs.motivationalEnabled,
                onCheckedChange = onSetMotivational,
                testTag = "pref_motivational_notifications"
            )
            PreferenceToggleRow(
                title = "Quiet Hours (22:00 – 07:00)",
                subtitle = "Pause promotional and discovery alerts overnight.",
                checked = prefs.quietHoursEnabled,
                onCheckedChange = onSetQuietHours,
                testTag = "pref_quiet_hours"
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag)
        )
    }
}
