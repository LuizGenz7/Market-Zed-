package com.example.ui.screens.post

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import coil.compose.AsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.DateUtils
import com.example.core.util.PhoneValidator
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.ListingDraft
import com.example.data.model.ListingLocation
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WantedBlue
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListingScreen(
    state: PostListingUiState,
    onRequireSignIn: () -> Unit,
    onOpenActivation: () -> Unit,
    onRefreshProfile: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onUpdateDraft: ((ListingDraft) -> ListingDraft) -> Unit,
    onSetStep: (Int) -> Unit,
    onPickImages: (List<android.net.Uri>) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onRetryPhotoUpload: (String) -> Unit,
    onSaveDraftNow: () -> Unit,
    onClearDraftNotice: () -> Unit,
    onLoadDraft: (ListingDraft) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onStartNewDraft: () -> Unit,
    onSubmitListing: () -> Unit,
    onViewPublishedListing: (String) -> Unit,
    onOpenMyListings: () -> Unit,
    onReturnHome: () -> Unit,
    onAcknowledgeWantedGate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onPickImages(uris)
        }
    }

    var showDraftsSheet by remember { mutableStateOf(false) }
    val snackbar = LocalTopSnackbarController.current

    LaunchedEffect(state.draftSavedNotice) {
        val msg = state.draftSavedNotice?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showSuccess(msg)
        onClearDraftNotice()
    }

    LaunchedEffect(state.profileError) {
        val err = state.profileError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showWarning(
            message = "Could not verify posting access: $err",
            actionLabel = "Retry",
            onAction = onRefreshProfile
        )
    }

    LaunchedEffect(state.submitError) {
        val err = state.submitError?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showError(err)
    }

    LaunchedEffect(state.ambiguousSubmitMessage) {
        val msg = state.ambiguousSubmitMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showWarning(
            message = msg,
            actionLabel = "Check",
            onAction = onOpenMyListings
        )
    }

    // Published Success State
    if (!state.publishedListingId.isNullOrBlank()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_circle_check),
                contentDescription = "Listing live",
                tint = VerifiedEmerald,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your listing is now live.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your listing is publicly visible on MarketZed immediately. Administrators will review it after publication.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onViewPublishedListing(state.publishedListingId) },
                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Listing")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenMyListings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage My Listings")
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    onStartNewDraft()
                    onReturnHome()
                }
            ) {
                Text("Return Home")
            }
        }
        return
    }

    // Signed-out Gate (still allows managing offline drafts notice, then routes to login)
    if (!state.isSignedIn) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val feeLabel = PriceFormatter.formatKwachaAmount(state.verificationFeeZmw)
            EmptyStateView(
                iconRes = LucideR.drawable.lucide_ic_shield_check,
                title = "Sign in to post on MarketZed",
                message = "Your first Wanted request is FREE! Publishing Products, Rental Properties, Services, Swaps, Jobs, or 2nd+ Wanted requests requires a one-time $feeLabel account verification.",
                actionLabel = "Sign In or Create Account",
                onAction = onRequireSignIn
            )
        }
        return
    }

    val draft = state.draft
    val selectedCategory = state.categories.firstOrNull { it.id == draft.categoryId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Local Drafts toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Post a Listing",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saved automatically on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                if (state.savedDrafts.isNotEmpty()) {
                    TextButton(onClick = { showDraftsSheet = !showDraftsSheet }) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_file_text),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Drafts (${state.savedDrafts.size})")
                    }
                }
                TextButton(onClick = onSaveDraftNow) {
                    Text("Save Draft", color = MarketZedOrange)
                }
            }
        }


        // Collapsible Device-Local Drafts Drawer
        if (showDraftsSheet && state.savedDrafts.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Saved Local Drafts (device-only)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    state.savedDrafts.forEach { saved ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLoadDraft(saved)
                                        showDraftsSheet = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = saved.title.ifBlank { "Untitled ${MarketZedCatalogue.categoryLabel(saved.categoryId)}" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Updated ${DateUtils.formatRelativeTime(saved.updatedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteDraft(saved.localUuid) }) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_trash_2),
                                        contentDescription = "Delete draft"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verification & Wanted 1-Free-Post Gate Banner
        val feeLabel = PriceFormatter.formatKwachaAmount(state.verificationFeeZmw)
        if (state.profileLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MarketZedOrange
            )
        } else if (!state.profileError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Could not verify posting access: ${state.profileError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRefreshProfile) { Text("Retry") }
                }
            }
        } else if (state.isWantedSelected) {
            when {
                state.isVerifiedAccount -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VerifiedEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                contentDescription = null,
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Verified Account · Unlimited text-only Wanted requests unlocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                state.isFirstFreeWantedEligible -> {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WantedBlue.copy(alpha = 0.10f)),
                        border = BorderStroke(1.dp, WantedBlue.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_wanted_free_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_handshake),
                                    contentDescription = "Free Wanted post",
                                    tint = WantedBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Your first Wanted post is FREE!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WantedBlue
                                )
                            }
                            Text(
                                text = "Tell Zambia what you are looking for — text only, no photos or payment required for your first Wanted request.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!state.wantedGateAcknowledged) {
                                Button(
                                    onClick = onAcknowledgeWantedGate,
                                    colors = ButtonDefaults.buttonColors(containerColor = WantedBlue),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("post_wanted_free_continue_button")
                                ) {
                                    Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                state.needsVerificationForNextWanted -> {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, MarketZedOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_wanted_unlock_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                    contentDescription = null,
                                    tint = MarketZedOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "UNLOCK MORE WANTED POSTS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MarketZedOrange
                                )
                            }
                            Text(
                                text = "You have used your 1 free Wanted post. Verify your account once ($feeLabel) to unlock unlimited Wanted posts and all MarketZed categories.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenActivation,
                                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("post_wanted_verify_button")
                                ) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify Account", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(onClick = onRefreshProfile) {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_refresh_cw),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Recheck")
                                }
                            }
                        }
                    }
                }
            }
        } else if (state.profile != null && !state.isVerifiedAccount) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, MarketZedOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "One-time $feeLabel Account Verification Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Publishing in ${selectedCategory?.name ?: "normal categories"} requires a one-time $feeLabel Lipila mobile-money verification. You can still save your draft now, or use your 1 free Wanted post.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenActivation,
                            colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                            modifier = Modifier.testTag("post_activate_k30_button")
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify ($feeLabel)", color = Color.White)
                        }
                        OutlinedButton(onClick = onRefreshProfile) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_refresh_cw),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recheck")
                        }
                    }
                }
            }
        }

        // Step Progress Bar (Wanted skips Photos cleanly)
        val stepItems = if (state.isWantedSelected) {
            listOf(0 to "Details", 2 to "Location", 3 to "Publish")
        } else {
            listOf(0 to "Details", 1 to "Photos", 2 to "Location", 3 to "Publish")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stepItems.forEachIndexed { displayIndex, (stepKey, title) ->
                val active = draft.step == stepKey
                val completed = draft.step > stepKey
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        active -> MarketZedOrange
                        completed -> VerifiedEmerald.copy(alpha = 0.16f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSetStep(stepKey) }
                ) {
                    Text(
                        text = "${displayIndex + 1}. $title",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            active -> Color.White
                            completed -> VerifiedEmerald
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Step Content
        when {
            draft.step == 0 -> StepDetailsForm(
                state = state,
                onSelectCategory = onSelectCategory,
                onUpdateDraft = onUpdateDraft
            )
            draft.step == 1 && !state.isWantedSelected -> StepPhotosForm(
                state = state,
                onPickPhotos = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemovePhoto = onRemovePhoto,
                onRetryUpload = onRetryPhotoUpload
            )
            draft.step == 1 && state.isWantedSelected -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WantedBlue.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, WantedBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_handshake),
                            contentDescription = null,
                            tint = WantedBlue
                        )
                        Text(
                            text = "Wanted requests are text-only and do not use photos. Tap Next Step to continue to Location & Contact.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            draft.step == 2 -> StepLocationAndContactForm(
                state = state,
                onUpdateDraft = onUpdateDraft
            )
            else -> StepReviewAndPublishForm(
                state = state,
                onUpdateDraft = onUpdateDraft,
                onOpenMyListings = onOpenMyListings,
                onOpenActivation = onOpenActivation,
                onSubmit = onSubmitListing
            )
        }

        // Step Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (draft.step > 0) {
                OutlinedButton(
                    onClick = {
                        val prev = if (state.isWantedSelected && draft.step == 2) 0 else draft.step - 1
                        onSetStep(prev)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (draft.step < 3) {
                Button(
                    onClick = {
                        val next = if (state.isWantedSelected && draft.step == 0) 2 else draft.step + 1
                        onSetStep(next)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("post_next_step_button")
                ) {
                    Text("Next Step")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepDetailsForm(
    state: PostListingUiState,
    onSelectCategory: (String) -> Unit,
    onUpdateDraft: ((ListingDraft) -> ListingDraft) -> Unit
) {
    val draft = state.draft
    val selectedCat = state.categories.firstOrNull { it.id == draft.categoryId }
    var subcatExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.categories) { cat ->
                val isWanted = cat.id == "wanted"
                val wantedChipLabel = when {
                    !isWanted -> cat.name
                    !state.isVerifiedAccount && !state.hasUsedFreeWantedPost -> "Wanted (1st Free)"
                    else -> cat.name
                }
                FilterChip(
                    selected = draft.categoryId == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    leadingIcon = if (isWanted) {
                        {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_handshake),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    label = { Text(wantedChipLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isWanted) WantedBlue else MarketZedOrange,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    modifier = Modifier.testTag("post_cat_${cat.id}")
                )
            }
        }

        if (selectedCat != null) {
            ExposedDropdownMenuBox(
                expanded = subcatExpanded,
                onExpandedChange = { subcatExpanded = it }
            ) {
                MarketZedOutlinedTextField(
                    value = draft.subcategory.ifBlank { "Select subcategory" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subcategory *") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_layers),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isError = state.validationErrors.containsKey("subcategory"),
                    supportingText = state.validationErrors["subcategory"]?.let { err -> { Text(err) } },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subcatExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = subcatExpanded,
                    onDismissRequest = { subcatExpanded = false }
                ) {
                    selectedCat.subcategories.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub) },
                            onClick = {
                                onUpdateDraft { it.copy(subcategory = sub) }
                                subcatExpanded = false
                            }
                        )
                    }
                }
            }
        }

        MarketZedOutlinedTextField(
            value = draft.title,
            onValueChange = { value -> if (value.length <= 120) onUpdateDraft { it.copy(title = value) } },
            label = { Text("Listing Title * (4–120 chars)") },
            leadingIcon = {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_tag),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            isError = state.validationErrors.containsKey("title"),
            supportingText = {
                Text(state.validationErrors["title"] ?: "${draft.title.length}/120")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_title_input")
        )

        MarketZedOutlinedTextField(
            value = draft.description,
            onValueChange = { value -> if (value.length <= 5000) onUpdateDraft { it.copy(description = value) } },
            label = { Text("Description * (20–5,000 chars)") },
            leadingIcon = {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_file_text),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            isError = state.validationErrors.containsKey("description"),
            supportingText = {
                Text(state.validationErrors["description"] ?: "${draft.description.length}/5000")
            },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_description_input")
        )

        MarketZedOutlinedTextField(
            value = draft.price,
            onValueChange = { value -> onUpdateDraft { it.copy(price = value) } },
            label = {
                Text(
                    when (draft.categoryId) {
                        "wanted" -> "Budget in ZMW (optional)"
                        "free" -> "Price (enter 0 or leave blank for Free)"
                        else -> "Price in ZMW / K (optional)"
                    }
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_banknote),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = state.validationErrors.containsKey("price"),
            supportingText = state.validationErrors["price"]?.let { err -> { Text(err) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_price_input")
        )

        // Category-specific fields
        when (draft.categoryId) {
            "marketplace" -> {
                Text("Condition (optional)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MarketZedCatalogue.conditionOptions) { cond ->
                        FilterChip(
                            selected = draft.categoryDetails.condition == cond,
                            onClick = {
                                onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(condition = cond)) }
                            },
                            label = { Text(cond) }
                        )
                    }
                }
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.brand.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(brand = v)) }
                    },
                    label = { Text("Brand (optional)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_tag),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("post_brand_input")
                )
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.model.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(model = v)) }
                    },
                    label = { Text("Model (optional)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_package),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("post_model_input")
                )
                val extraSpecKeys = MarketZedCatalogue.comparisonSpecKeysBySubcategory[draft.subcategory]
                    ?.filter { it !in setOf("Brand", "Model", "Condition") }
                    .orEmpty()
                if (extraSpecKeys.isNotEmpty()) {
                    Text(
                        text = "Product Specifications (optional — helps buyers compare)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val currentSpecs = draft.categoryDetails.specs.orEmpty()
                    extraSpecKeys.forEach { specKey ->
                        MarketZedOutlinedTextField(
                            value = currentSpecs[specKey].orEmpty(),
                            onValueChange = { v ->
                                val nextSpecs = currentSpecs.toMutableMap().apply {
                                    if (v.isBlank()) remove(specKey) else put(specKey, v)
                                }
                                onUpdateDraft {
                                    it.copy(
                                        categoryDetails = it.categoryDetails.copy(
                                            specs = nextSpecs.takeIf { map -> map.isNotEmpty() }
                                        )
                                    )
                                }
                            },
                            label = { Text("$specKey (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            "property" -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MarketZedOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "For Rent Only: Houses, apartments, rooms, shops, offices, commercial spaces, and warehouses for rent. Land, plots, and properties for sale are not permitted.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.bedrooms.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(bedrooms = v)) }
                    },
                    label = { Text("Bedrooms (optional)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_bed),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.bathrooms.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(bathrooms = v)) }
                    },
                    label = { Text("Bathrooms (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.amenities.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(amenities = v)) }
                    },
                    label = { Text("Amenities (e.g. Borehole, Carport, Paved yard)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "jobs" -> {
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.employerName.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(employerName = v)) }
                    },
                    label = { Text("Employer / Company Name (optional)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_briefcase),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Employment Type (optional)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MarketZedCatalogue.employmentTypeOptions) { emp ->
                        FilterChip(
                            selected = draft.categoryDetails.employmentType == emp,
                            onClick = {
                                onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(employmentType = emp)) }
                            },
                            label = { Text(emp) }
                        )
                    }
                }
            }
            "swaps" -> {
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.swapFor.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(swapFor = v)) }
                    },
                    label = { Text("What would you like to swap for? (optional)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_arrow_left_right),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "services" -> {
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.serviceArea.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(serviceArea = v)) }
                    },
                    label = { Text("Service Coverage Area (optional)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_compass),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "wanted" -> {
                MarketZedOutlinedTextField(
                    value = draft.categoryDetails.wantedDetails.orEmpty(),
                    onValueChange = { v ->
                        onUpdateDraft { it.copy(categoryDetails = it.categoryDetails.copy(wantedDetails = v)) }
                    },
                    label = { Text("Preferred specs / timeline (optional)") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_handshake),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StepPhotosForm(
    state: PostListingUiState,
    onPickPhotos: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onRetryUpload: (String) -> Unit
) {
    val cat = MarketZedCatalogue.categoryById(state.draft.categoryId)
    val required = cat?.requiresImage == true

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (required) "Photos * (1 to 10 images, max 8 MB each)" else "Photos (optional, up to 10 images)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Supported formats: JPG, PNG, WebP. Images are stored safely in your local draft until published.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onPickPhotos,
            enabled = state.photos.size < 10,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("post_add_photos_button")
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_image_plus),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Photos (${state.photos.size}/10)")
        }

        state.validationErrors["images"]?.let { err ->
            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        state.photos.forEach { photo ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = File(photo.localPath),
                        contentDescription = "Draft photo preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        when (val st = photo.uploadState) {
                            DraftImageUploadState.Pending ->
                                Text("Waiting to upload", style = MaterialTheme.typography.bodySmall)
                            DraftImageUploadState.Uploading -> {
                                Text("Uploading...", style = MaterialTheme.typography.bodySmall, color = MarketZedOrange)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MarketZedOrange)
                            }
                            is DraftImageUploadState.Uploaded ->
                                Text("Uploaded to MarketZed", style = MaterialTheme.typography.bodySmall, color = VerifiedEmerald)
                            is DraftImageUploadState.Failed -> {
                                Text(st.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = { onRetryUpload(photo.localPath) }) {
                                    Text("Retry Upload")
                                }
                            }
                        }
                    }
                    IconButton(onClick = { onRemovePhoto(photo.localPath) }) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_x),
                            contentDescription = "Remove photo"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepLocationAndContactForm(
    state: PostListingUiState,
    onUpdateDraft: ((ListingDraft) -> ListingDraft) -> Unit
) {
    val draft = state.draft
    var provinceExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var townExpanded by remember { mutableStateOf(false) }
    val districts = MarketZedCatalogue.districtsForProvince(draft.province)
    val towns = MarketZedCatalogue.townsForProvince(draft.province)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Location in Zambia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        ExposedDropdownMenuBox(
            expanded = provinceExpanded,
            onExpandedChange = { provinceExpanded = it }
        ) {
            MarketZedOutlinedTextField(
                value = draft.province,
                onValueChange = {},
                readOnly = true,
                label = { Text("Province *") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                isError = state.validationErrors.containsKey("province"),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = provinceExpanded,
                onDismissRequest = { provinceExpanded = false }
            ) {
                state.provinces.forEach { prov ->
                    DropdownMenuItem(
                        text = { Text(prov.province) },
                        onClick = {
                            val defaultDistrict = MarketZedCatalogue.districtsForProvince(prov.province)
                                .firstOrNull().orEmpty()
                            onUpdateDraft {
                                it.copy(
                                    province = prov.province,
                                    district = defaultDistrict,
                                    town = prov.towns.firstOrNull().orEmpty()
                                )
                            }
                            provinceExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = districtExpanded,
            onExpandedChange = { districtExpanded = it }
        ) {
            MarketZedOutlinedTextField(
                value = draft.district,
                onValueChange = { v -> onUpdateDraft { it.copy(district = v) } },
                label = { Text("District (optional)") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_map_pinned),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(
                expanded = districtExpanded,
                onDismissRequest = { districtExpanded = false }
            ) {
                districts.forEach { dName ->
                    DropdownMenuItem(
                        text = { Text(dName) },
                        onClick = {
                            onUpdateDraft { it.copy(district = dName) }
                            districtExpanded = false
                        }
                    )
                }
            }
        }

        if (towns.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = townExpanded,
                onExpandedChange = { townExpanded = it }
            ) {
                MarketZedOutlinedTextField(
                    value = draft.town,
                    onValueChange = { v -> onUpdateDraft { it.copy(town = v) } },
                    label = { Text("Town *") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_map_pinned),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isError = state.validationErrors.containsKey("town"),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = townExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                )
                ExposedDropdownMenu(
                    expanded = townExpanded,
                    onDismissRequest = { townExpanded = false }
                ) {
                    towns.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                onUpdateDraft { it.copy(town = t) }
                                townExpanded = false
                            }
                        )
                    }
                }
            }
        } else {
            MarketZedOutlinedTextField(
                value = draft.town,
                onValueChange = { v -> onUpdateDraft { it.copy(town = v) } },
                label = { Text("Town *") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_map_pinned),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        MarketZedOutlinedTextField(
            value = draft.nearbyArea,
            onValueChange = { v -> onUpdateDraft { it.copy(nearbyArea = v) } },
            label = { Text("Nearby area / Compound (optional)") },
            placeholder = { Text("e.g. Woodlands, Kabulonga, Kamwala South") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text("Contact Preferences (at least one required)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        state.validationErrors["contact"]?.let { err ->
            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        val phoneValue = draft.contactPreferences.phone.orEmpty()
        val phonePreview = PhoneValidator.normalizeZambianNumber(phoneValue)?.takeIf { it != phoneValue.trim() }
        MarketZedOutlinedTextField(
            value = phoneValue,
            onValueChange = { v ->
                onUpdateDraft { it.copy(contactPreferences = it.contactPreferences.copy(phone = v)) }
            },
            label = { Text("Phone (e.g. 969333123 or 0969331234)") },
            leadingIcon = {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_phone),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.validationErrors.containsKey("phone"),
            supportingText = state.validationErrors["phone"]?.let { err -> { Text(err) } }
                ?: phonePreview?.let { fixed -> { Text("Auto-formats to $fixed") } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && phoneValue.isNotBlank()) {
                        val fixed = PhoneValidator.autoFixZambianInput(phoneValue)
                        if (fixed != phoneValue) {
                            onUpdateDraft { it.copy(contactPreferences = it.contactPreferences.copy(phone = fixed)) }
                        }
                    }
                }
        )

        val waValue = draft.contactPreferences.whatsapp.orEmpty()
        val waPreview = PhoneValidator.normalizeZambianNumber(waValue)?.takeIf { it != waValue.trim() }
        MarketZedOutlinedTextField(
            value = waValue,
            onValueChange = { v ->
                onUpdateDraft { it.copy(contactPreferences = it.contactPreferences.copy(whatsapp = v)) }
            },
            label = { Text("WhatsApp (e.g. 969333123 or 0969331234)") },
            leadingIcon = {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_message_circle),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.validationErrors.containsKey("whatsapp"),
            supportingText = state.validationErrors["whatsapp"]?.let { err -> { Text(err) } }
                ?: waPreview?.let { fixed -> { Text("Auto-formats to $fixed") } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && waValue.isNotBlank()) {
                        val fixed = PhoneValidator.autoFixZambianInput(waValue)
                        if (fixed != waValue) {
                            onUpdateDraft { it.copy(contactPreferences = it.contactPreferences.copy(whatsapp = fixed)) }
                        }
                    }
                }
        )

        MarketZedOutlinedTextField(
            value = draft.contactPreferences.email.orEmpty(),
            onValueChange = { v ->
                onUpdateDraft { it.copy(contactPreferences = it.contactPreferences.copy(email = v)) }
            },
            label = { Text("Public Contact Email") },
            leadingIcon = {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_mail),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.validationErrors.containsKey("email"),
            supportingText = state.validationErrors["email"]?.let { err -> { Text(err) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepReviewAndPublishForm(
    state: PostListingUiState,
    onUpdateDraft: ((ListingDraft) -> ListingDraft) -> Unit,
    onOpenMyListings: () -> Unit,
    onOpenActivation: () -> Unit,
    onSubmit: () -> Unit
) {
    val draft = state.draft
    val locationLabel = ListingLocation(
        province = draft.province,
        town = draft.town,
        district = draft.district,
        nearbyArea = draft.nearbyArea.takeIf { it.isNotBlank() }
    ).displayFull

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review & Publish", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = draft.title.ifBlank { "No title entered" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${MarketZedCatalogue.categoryLabel(draft.categoryId)} · ${draft.subcategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MarketZedOrange
                )
                Text(
                    text = if (state.isWantedSelected) {
                        "$locationLabel · Text-only request"
                    } else {
                        "$locationLabel · ${state.photos.size} photo(s)"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Exact Ownership Declaration (2026-11-16-v1, NEVER pre-checked)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onUpdateDraft {
                        it.copy(
                            ownershipDeclarationAccepted = !it.ownershipDeclarationAccepted,
                            ownershipDeclarationVersion = MarketZedCatalogue.OWNERSHIP_DECLARATION_VERSION
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = draft.ownershipDeclarationAccepted,
                    onCheckedChange = { checked ->
                        onUpdateDraft {
                            it.copy(
                                ownershipDeclarationAccepted = checked,
                                ownershipDeclarationVersion = MarketZedCatalogue.OWNERSHIP_DECLARATION_VERSION
                            )
                        }
                    },
                    modifier = Modifier.testTag("ownership_declaration_checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = MarketZedCatalogue.OWNERSHIP_DECLARATION_TEXT,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Declaration version: ${MarketZedCatalogue.OWNERSHIP_DECLARATION_VERSION}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.validationErrors.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    state.validationErrors.values.forEach { msg ->
                        Text("• $msg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        if (!state.submitError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_circle_alert),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.submitError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (!state.ambiguousSubmitMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Submission Status Unconfirmed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.ambiguousSubmitMessage,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onOpenMyListings) {
                        Text("Check My Listings")
                    }
                }
            }
        }

        if (!state.isEligibleToPublishSelectedCategory) {
            OutlinedButton(
                onClick = onOpenActivation,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                    contentDescription = null,
                    tint = MarketZedOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify Account (${PriceFormatter.formatKwachaAmount(state.verificationFeeZmw)})",
                    color = MarketZedOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting &&
                state.isEligibleToPublishSelectedCategory &&
                state.ambiguousSubmitMessage == null,
            colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("post_publish_button")
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publishing...")
            } else {
                Text(
                    text = when {
                        state.needsVerificationForNextWanted -> "Verify Account to Unlock More Wanted Posts"
                        state.isFirstFreeWantedEligible -> "Publish Free Wanted Request"
                        state.isWantedSelected -> "Publish Wanted Request"
                        else -> "Publish Listing"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
