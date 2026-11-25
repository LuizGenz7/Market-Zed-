package com.example.ui.screens.account

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.StatusBadgePill
import com.example.ui.components.UserAvatarBadge
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber

enum class AccountDestination {
    DASHBOARD,
    ADMIN_DASHBOARD,
    PROFILE_EDIT,
    ACTIVATION,
    MY_LISTINGS,
    PROMOTIONS,
    PAYMENT_HISTORY,
    NOTIFICATIONS,
    SAVED,
    SETTINGS,
    ABOUT,
    SAFETY,
    PRIVACY,
    TERMS
}

@Composable
fun AccountScreen(
    state: AccountUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onRegister: (fullName: String, email: String, password: String, confirm: String) -> Unit,
    onPasswordReset: (email: String) -> Unit,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateSubScreen: (AccountDestination) -> Unit,
    onClearAuthBanners: () -> Unit,
    onClearSessionExpiredPrompt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.authError) {
        val err = state.authError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showError(err)
        onClearAuthBanners()
    }

    LaunchedEffect(state.authMessage) {
        val msg = state.authMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showSuccess(msg)
        onClearAuthBanners()
    }

    if (!state.auth.isSignedIn) {
        AuthGateView(
            state = state,
            onSignIn = onSignIn,
            onRegister = onRegister,
            onPasswordReset = onPasswordReset,
            onNavigateStatic = onNavigateSubScreen,
            onClearAuthBanners = onClearAuthBanners,
            onClearSessionExpiredPrompt = onClearSessionExpiredPrompt,
            modifier = modifier
        )
        return
    }

    val profile = state.profile
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // User Identity Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val resolvedDisplayName = profile?.displayName?.takeIf { it.isNotBlank() }
                            ?: state.auth.displayName?.takeIf { it.isNotBlank() }
                            ?: "MarketZed User"
                        UserAvatarBadge(
                            photoUrl = state.auth.photoUrl,
                            displayName = resolvedDisplayName,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resolvedDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.auth.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (profile?.hasVerifiedEntitlement == true) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                contentDescription = "Verified account",
                                tint = VerifiedEmerald
                            )
                        }
                    }

                    val basicInfoSummary = listOfNotNull(
                        profile?.contactNumber?.takeIf { it.isNotBlank() },
                        listOfNotNull(
                            profile?.town?.takeIf { it.isNotBlank() },
                            profile?.province?.takeIf { it.isNotBlank() }
                        ).joinToString(", ").takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (basicInfoSummary.isNotBlank()) {
                        Text(
                            text = basicInfoSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    state.sessionExpiredPrompt?.let { prompt ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningAmber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth().testTag("account_session_expired_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = onSignOut) {
                                        Text("Sign in again", color = MarketZedOrange, fontWeight = FontWeight.SemiBold)
                                    }
                                    TextButton(onClick = onClearSessionExpiredPrompt) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }

                    // Email verification status (kept clearly separate from account verification fee)
                    if (!state.auth.isEmailVerified) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningAmber.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Email not verified yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onResendVerification) { Text("Resend") }
                                TextButton(onClick = onRefreshVerification) { Text("Check") }
                            }
                        }
                    }
                }
            }
        }

        // Prominent Full-Width Verify Account CTA immediately below basic info card
        item {
            val verificationStatus = profile?.resolvedVerificationStatus
                ?: com.example.data.model.AccountVerificationUiStatus.NOT_VERIFIED
            val feeLabel = com.example.core.util.PriceFormatter.formatKwachaAmount(state.verificationFeeZmw)

            when (verificationStatus) {
                com.example.data.model.AccountVerificationUiStatus.VERIFIED -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VerifiedEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.2.dp, VerifiedEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateSubScreen(AccountDestination.ACTIVATION) }
                            .testTag("account_verify_cta")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = VerifiedEmerald,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                        contentDescription = "Verified account",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verified Account",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedEmerald
                                )
                                Text(
                                    text = "Full posting access & unlimited Wanted requests unlocked.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                                contentDescription = null,
                                tint = VerifiedEmerald
                            )
                        }
                    }
                }
                com.example.data.model.AccountVerificationUiStatus.PENDING -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = WarningAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.2.dp, WarningAmber.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateSubScreen(AccountDestination.ACTIVATION) }
                            .testTag("account_verify_cta")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MarketZedOrange,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                        contentDescription = "Verification pending",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verification Pending · Check Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your $feeLabel mobile money verification is awaiting confirmation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                                contentDescription = null,
                                tint = MarketZedOrange
                            )
                        }
                    }
                }
                com.example.data.model.AccountVerificationUiStatus.FAILED -> {
                    Button(
                        onClick = { onNavigateSubScreen(AccountDestination.ACTIVATION) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("account_verify_cta")
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                            contentDescription = "Retry account verification",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verification Failed · Try Again",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tap to retry one-time $feeLabel verification or contact support.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                com.example.data.model.AccountVerificationUiStatus.NOT_VERIFIED -> {
                    Button(
                        onClick = { onNavigateSubScreen(AccountDestination.ACTIVATION) },
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("account_verify_cta")
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                            contentDescription = "Verify Account",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verify Account · $feeLabel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Unlock all posting categories & unlimited Wanted requests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Admin-Only Control Center (strictly hidden from non-admin accounts)
        if (state.isAdmin) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VerifiedEmerald.copy(alpha = 0.1f),
                    border = BorderStroke(1.2.dp, VerifiedEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_admin_dashboard")
                        .clickable { onNavigateSubScreen(AccountDestination.ADMIN_DASHBOARD) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VerifiedEmerald.copy(alpha = 0.18f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                    contentDescription = "Admin Dashboard",
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Admin Dashboard",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusBadgePill(
                                    text = "ADMIN ONLY",
                                    backgroundColor = VerifiedEmerald,
                                    textColor = Color.White
                                )
                            }
                            Text(
                                text = "Moderate listings, promotions, reports, users & payments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                            contentDescription = null,
                            tint = VerifiedEmerald
                        )
                    }
                }
            }
        }

        // Dashboard Menu
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_package,
                    title = "My Listings",
                    subtitle = "Manage availability, revise, fulfill & renew wanted",
                    onClick = { onNavigateSubScreen(AccountDestination.MY_LISTINGS) },
                    testTag = "account_my_listings"
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_bookmark,
                    title = "Saved & History",
                    subtitle = "Saved listings and recently viewed items",
                    onClick = { onNavigateSubScreen(AccountDestination.SAVED) },
                    testTag = "account_saved_history"
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_megaphone,
                    title = "Promote Business & Listings",
                    subtitle = "Sponsor products, rentals & services (7, 14, or 30 days)",
                    onClick = { onNavigateSubScreen(AccountDestination.PROMOTIONS) },
                    testTag = "account_promotions"
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_credit_card,
                    title = "Payment History",
                    subtitle = "Your K30 activation & promotion Lipila receipts",
                    onClick = { onNavigateSubScreen(AccountDestination.PAYMENT_HISTORY) },
                    testTag = "account_payments"
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_bell,
                    title = "Notifications",
                    subtitle = "Listing, activation & promotion updates",
                    badgeCount = state.unreadNotificationCount,
                    onClick = { onNavigateSubScreen(AccountDestination.NOTIFICATIONS) },
                    testTag = "account_notifications"
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_user,
                    title = "Edit Profile",
                    subtitle = "Name, phone, WhatsApp, province & business type",
                    onClick = { onNavigateSubScreen(AccountDestination.PROFILE_EDIT) },
                    testTag = "account_profile_edit"
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_settings,
                    title = "App Settings & Offline Cache",
                    subtitle = "Theme, Data Saver, cache management & diagnostics",
                    onClick = { onNavigateSubScreen(AccountDestination.SETTINGS) },
                    testTag = "account_settings"
                )
            }
        }

        // Info & Policies
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Marketplace Info & Policies",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_shield,
                    title = "Safety Guidance",
                    subtitle = "Tips for meeting safely & avoiding scams",
                    onClick = { onNavigateSubScreen(AccountDestination.SAFETY) }
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_info,
                    title = "About MarketZed",
                    subtitle = "App by Tech Generative Universe (TechGU)",
                    onClick = { onNavigateSubScreen(AccountDestination.ABOUT) }
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_shield_check,
                    title = "Privacy Policy",
                    subtitle = "How MarketZed handles account and listing data",
                    onClick = { onNavigateSubScreen(AccountDestination.PRIVACY) }
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_file_text,
                    title = "Terms & Conditions",
                    subtitle = "Rules for using MarketZed in Zambia",
                    onClick = { onNavigateSubScreen(AccountDestination.TERMS) }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { showSignOutConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("account_sign_out_button")
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_log_out),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out") },
            text = { Text("You will be signed out of your MarketZed account on this device. Your device-local drafts for this account stay protected.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AuthGateView(
    state: AccountUiState,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onPasswordReset: (String) -> Unit,
    onNavigateStatic: (AccountDestination) -> Unit,
    onClearAuthBanners: () -> Unit,
    onClearSessionExpiredPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isRegisterMode) "Create MarketZed Account" else "Sign in to MarketZed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sign in with your MarketZed email and password across web and mobile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.sessionExpiredPrompt?.let { prompt ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("auth_session_expired_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearSessionExpiredPrompt) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        if (!state.auth.isFirebaseConfigured) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_shield_alert),
                            contentDescription = null,
                            tint = WarningAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firebase Authentication config (google-services.json) is not bundled in this preview build yet. Public browsing, offline cache, saved items, and settings remain active.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }


        if (isRegisterMode) {
            item {
                MarketZedOutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_user),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_full_name_input")
                )
            }
        }

        item {
            MarketZedOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_mail),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
            )
        }

        item {
            MarketZedOutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isRegisterMode) "Password (8+ characters)" else "Password") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("auth_password_visibility_toggle")
                    ) {
                        Icon(
                            painter = painterResource(
                                if (passwordVisible) {
                                    LucideR.drawable.lucide_ic_eye_off
                                } else {
                                    LucideR.drawable.lucide_ic_eye
                                }
                            ),
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
            )
        }

        if (isRegisterMode) {
            item {
                MarketZedOutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_lock_keyhole),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("auth_confirm_password_visibility_toggle")
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (confirmPasswordVisible) {
                                        LucideR.drawable.lucide_ic_eye_off
                                    } else {
                                        LucideR.drawable.lucide_ic_eye
                                    }
                                ),
                                contentDescription = if (confirmPasswordVisible) "Hide confirm password" else "Show confirm password",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input")
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (isRegisterMode) {
                        onRegister(fullName, email, password, confirmPassword)
                    } else {
                        onSignIn(email, password)
                    }
                },
                enabled = !state.authBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("auth_submit_button")
            ) {
                if (state.authBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(if (isRegisterMode) "Create Account" else "Sign In", fontWeight = FontWeight.SemiBold)
                }
            }
        }



        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        onClearAuthBanners()
                        isRegisterMode = !isRegisterMode
                    }
                ) {
                    Text(
                        if (isRegisterMode) "Already have an account? Sign In" else "Create an account",
                        color = MarketZedOrange
                    )
                }
                if (!isRegisterMode) {
                    TextButton(onClick = { onPasswordReset(email) }) {
                        Text("Forgot password?")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "App Settings & Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_settings,
                    title = "App Settings & Offline Cache",
                    subtitle = "Theme, Data Saver & cache diagnostics",
                    onClick = { onNavigateStatic(AccountDestination.SETTINGS) }
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_shield,
                    title = "Safety Guidance",
                    subtitle = "Trading safely on MarketZed",
                    onClick = { onNavigateStatic(AccountDestination.SAFETY) }
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_info,
                    title = "About MarketZed",
                    subtitle = "App by Tech Generative Universe (TechGU)",
                    onClick = { onNavigateStatic(AccountDestination.ABOUT) }
                )
                AccountMenuRow(
                    iconRes = LucideR.drawable.lucide_ic_shield_check,
                    title = "Privacy Policy & Terms",
                    subtitle = "Legal notices and data handling",
                    onClick = { onNavigateStatic(AccountDestination.PRIVACY) }
                )
            }
        }
    }
}

@Composable
fun AccountMenuRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    testTag: String? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MarketZedOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badgeCount > 0) {
                Badge(containerColor = MarketZedOrange) {
                    Text(badgeCount.coerceAtMost(99).toString())
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
