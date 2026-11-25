package com.example.ui.screens.account

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.PhoneValidator
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.repository.MutationResult
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.StatusBadgePill
import com.example.ui.components.WrapRow
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WantedBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingEditBottomSheet(
    listing: Listing,
    onDismiss: () -> Unit,
    onUploadImage: suspend (Uri) -> MutationResult<ListingImage>,
    onSaveChanges: (listingId: String, changes: Map<String, Any?>, onComplete: (Boolean, String) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isWanted = listing.isWanted
    val categoryKey = listing.categoryId.trim().lowercase()

    var subcategory by remember(listing.id) { mutableStateOf(listing.subcategory.orEmpty()) }
    var title by remember(listing.id) { mutableStateOf(listing.title) }
    var description by remember(listing.id) { mutableStateOf(listing.description) }
    var priceInput by remember(listing.id) { mutableStateOf(listing.price?.toString().orEmpty()) }
    var availabilityStatus by remember(listing.id) { mutableStateOf(listing.availabilityStatus ?: "available") }

    var province by remember(listing.id) { mutableStateOf(listing.location?.province.orEmpty().ifBlank { "Lusaka" }) }
    var district by remember(listing.id) { mutableStateOf(listing.location?.district.orEmpty()) }
    var town by remember(listing.id) { mutableStateOf(listing.location?.town.orEmpty()) }
    var nearbyArea by remember(listing.id) { mutableStateOf(listing.location?.nearbyArea.orEmpty()) }

    var condition by remember(listing.id) { mutableStateOf(listing.categoryDetails?.condition.orEmpty()) }
    var bedrooms by remember(listing.id) { mutableStateOf(listing.categoryDetails?.bedrooms.orEmpty()) }
    var bathrooms by remember(listing.id) { mutableStateOf(listing.categoryDetails?.bathrooms.orEmpty()) }
    var amenities by remember(listing.id) { mutableStateOf(listing.categoryDetails?.amenities.orEmpty()) }
    var serviceArea by remember(listing.id) { mutableStateOf(listing.categoryDetails?.serviceArea.orEmpty()) }
    var employerName by remember(listing.id) { mutableStateOf(listing.categoryDetails?.employerName.orEmpty()) }
    var employmentType by remember(listing.id) { mutableStateOf(listing.categoryDetails?.employmentType.orEmpty()) }
    var swapFor by remember(listing.id) { mutableStateOf(listing.categoryDetails?.swapFor.orEmpty()) }
    var wantedDetails by remember(listing.id) { mutableStateOf(listing.categoryDetails?.wantedDetails.orEmpty()) }

    var phone by remember(listing.id) { mutableStateOf(listing.contactPreferences?.phone.orEmpty()) }
    var whatsapp by remember(listing.id) { mutableStateOf(listing.contactPreferences?.whatsapp.orEmpty()) }
    var email by remember(listing.id) { mutableStateOf(listing.contactPreferences?.email.orEmpty()) }

    var images by remember(listing.id) { mutableStateOf(if (isWanted) emptyList() else listing.images) }
    var uploadingCount by remember { mutableIntStateOf(0) }
    var replaceIndex by remember { mutableIntStateOf(-1) }
    var imageError by remember { mutableStateOf<String?>(null) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val hasUnsavedChanges = remember(
        subcategory, title, description, priceInput, availabilityStatus,
        province, district, town, nearbyArea, condition, bedrooms, bathrooms,
        amenities, serviceArea, employerName, employmentType, swapFor, wantedDetails,
        phone, whatsapp, email, images, listing
    ) {
        subcategory != listing.subcategory.orEmpty() ||
            title != listing.title ||
            description != listing.description ||
            priceInput != listing.price?.toString().orEmpty() ||
            availabilityStatus != (listing.availabilityStatus ?: "available") ||
            province != listing.location?.province.orEmpty().ifBlank { "Lusaka" } ||
            district != listing.location?.district.orEmpty() ||
            town != listing.location?.town.orEmpty() ||
            nearbyArea != listing.location?.nearbyArea.orEmpty() ||
            condition != listing.categoryDetails?.condition.orEmpty() ||
            bedrooms != listing.categoryDetails?.bedrooms.orEmpty() ||
            bathrooms != listing.categoryDetails?.bathrooms.orEmpty() ||
            amenities != listing.categoryDetails?.amenities.orEmpty() ||
            serviceArea != listing.categoryDetails?.serviceArea.orEmpty() ||
            employerName != listing.categoryDetails?.employerName.orEmpty() ||
            employmentType != listing.categoryDetails?.employmentType.orEmpty() ||
            swapFor != listing.categoryDetails?.swapFor.orEmpty() ||
            wantedDetails != listing.categoryDetails?.wantedDetails.orEmpty() ||
            phone != listing.contactPreferences?.phone.orEmpty() ||
            whatsapp != listing.contactPreferences?.whatsapp.orEmpty() ||
            email != listing.contactPreferences?.email.orEmpty() ||
            (!isWanted && images != listing.images)
    }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (isWanted || uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            imageError = null
            val remaining = (10 - images.size).coerceAtLeast(0)
            uris.take(remaining).forEach { uri ->
                uploadingCount += 1
                when (val res = onUploadImage(uri)) {
                    is MutationResult.Success -> images = images + res.data
                    is MutationResult.Error -> imageError = res.message
                    is MutationResult.AmbiguousTimeout -> imageError = res.message
                }
                uploadingCount = (uploadingCount - 1).coerceAtLeast(0)
            }
        }
    }

    val replacePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val idx = replaceIndex
        replaceIndex = -1
        if (isWanted || uri == null || idx !in images.indices) return@rememberLauncherForActivityResult
        scope.launch {
            imageError = null
            uploadingCount += 1
            when (val res = onUploadImage(uri)) {
                is MutationResult.Success -> {
                    images = images.toMutableList().apply { set(idx, res.data) }
                }
                is MutationResult.Error -> imageError = res.message
                is MutationResult.AmbiguousTimeout -> imageError = res.message
            }
            uploadingCount = (uploadingCount - 1).coerceAtLeast(0)
        }
    }

    fun requestClose() {
        if (isSaving || uploadingCount > 0) return
        if (hasUnsavedChanges) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { requestClose() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp)
                .testTag("listing_edit_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edit ${MarketZedCatalogue.categoryLabel(listing.categoryId)} Listing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = { requestClose() },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_x),
                        contentDescription = "Close editor"
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subcategory Dropdown
                val subOptions = MarketZedCatalogue.categoryById(listing.categoryId)?.subcategories.orEmpty()
                if (subOptions.isNotEmpty()) {
                    var subExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = subExpanded,
                        onExpandedChange = { subExpanded = !subExpanded }
                    ) {
                        MarketZedOutlinedTextField(
                            value = subcategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subcategory *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subExpanded,
                            onDismissRequest = { subExpanded = false }
                        ) {
                            subOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        subcategory = option
                                        subExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Core Title & Description
                MarketZedOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_listing_title_input")
                )

                MarketZedOutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_listing_description_input")
                )

                if (categoryKey != "free") {
                    MarketZedOutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = {
                            Text(
                                when (categoryKey) {
                                    "property" -> "Monthly Rent (ZMW)"
                                    "jobs" -> "Salary / Pay (ZMW)"
                                    "wanted" -> "Target Budget (ZMW, optional)"
                                    "services" -> "Starting Price (ZMW, optional)"
                                    else -> "Price (ZMW)"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_listing_price_input")
                    )
                }

                // Category-Specific Fields
                when (categoryKey) {
                    "property" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MarketZedOrange.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MarketZedOrange.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Rentals-only policy: Property sales, land, stands, and plots are prohibited on MarketZed.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MarketZedOutlinedTextField(
                                value = bedrooms,
                                onValueChange = { bedrooms = it },
                                label = { Text("Bedrooms") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            MarketZedOutlinedTextField(
                                value = bathrooms,
                                onValueChange = { bathrooms = it },
                                label = { Text("Bathrooms") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MarketZedOutlinedTextField(
                            value = amenities,
                            onValueChange = { amenities = it },
                            label = { Text("Amenities (e.g. Borehole, Paved yard, Backup solar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "services" -> {
                        MarketZedOutlinedTextField(
                            value = serviceArea,
                            onValueChange = { serviceArea = it },
                            label = { Text("Service Area / Coverage") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "jobs" -> {
                        MarketZedOutlinedTextField(
                            value = employerName,
                            onValueChange = { employerName = it },
                            label = { Text("Employer / Organization Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Employment Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        WrapRow(horizontalSpacing = 8.dp, verticalSpacing = 6.dp) {
                            MarketZedCatalogue.employmentTypeOptions.forEach { empType ->
                                FilterChip(
                                    selected = employmentType.equals(empType, ignoreCase = true),
                                    onClick = { employmentType = empType },
                                    label = { Text(empType) }
                                )
                            }
                        }
                    }
                    "wanted" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WantedBlue.copy(alpha = 0.09f),
                            border = BorderStroke(1.dp, WantedBlue.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth().testTag("edit_wanted_text_only_notice")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_handshake),
                                    contentDescription = "Wanted request",
                                    tint = WantedBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Wanted listings are text-only and never include image uploads.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        MarketZedOutlinedTextField(
                            value = wantedDetails,
                            onValueChange = { wantedDetails = it },
                            label = { Text("Specific Requirements / Preferred Condition") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "swaps" -> {
                        MarketZedOutlinedTextField(
                            value = swapFor,
                            onValueChange = { swapFor = it },
                            label = { Text("What are you looking to swap for?") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        WrapRow(horizontalSpacing = 8.dp, verticalSpacing = 6.dp) {
                            MarketZedCatalogue.conditionOptions.forEach { cond ->
                                FilterChip(
                                    selected = condition.equals(cond, ignoreCase = true),
                                    onClick = { condition = cond },
                                    label = { Text(cond) }
                                )
                            }
                        }
                    }
                    else -> {
                        if (categoryKey != "free") {
                            Text("Condition", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                        WrapRow(horizontalSpacing = 8.dp, verticalSpacing = 6.dp) {
                            MarketZedCatalogue.conditionOptions.forEach { cond ->
                                FilterChip(
                                    selected = condition.equals(cond, ignoreCase = true),
                                    onClick = { condition = cond },
                                    label = { Text(cond) }
                                )
                            }
                        }
                    }
                }

                // Photos Manager (NEVER rendered for Wanted)
                if (!isWanted) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photos (${images.size}/10)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedButton(
                                onClick = {
                                    multiPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = images.size < 10 && uploadingCount == 0,
                                modifier = Modifier.testTag("edit_add_photos_button")
                            ) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_image_plus),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Photos")
                            }
                        }

                        if (uploadingCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MarketZedOrange)
                                Text("Uploading photo(s)...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        imageError?.let { err ->
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        if (images.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(images) { idx, img ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.width(136.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(92.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                            ) {
                                                AsyncImage(
                                                    model = img.thumbUrl ?: img.url,
                                                    contentDescription = "Listing photo ${idx + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxWidth().height(92.dp)
                                                )
                                                if (idx == 0) {
                                                    StatusBadgePill(
                                                        text = "Cover",
                                                        backgroundColor = MarketZedOrange,
                                                        textColor = Color.White,
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(4.dp)
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (idx > 0) {
                                                            images = images.toMutableList().apply {
                                                                val item = removeAt(idx)
                                                                add(idx - 1, item)
                                                            }
                                                        }
                                                    },
                                                    enabled = idx > 0,
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(LucideR.drawable.lucide_ic_arrow_left),
                                                        contentDescription = "Move photo left",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        replaceIndex = idx
                                                        replacePhotoPicker.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(LucideR.drawable.lucide_ic_refresh_cw),
                                                        contentDescription = "Replace photo",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        images = images.toMutableList().apply { removeAt(idx) }
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(LucideR.drawable.lucide_ic_trash_2),
                                                        contentDescription = "Remove photo",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
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

                // Location: Province -> District -> Town + Nearby Area
                Text("Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                var provExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = provExpanded,
                    onExpandedChange = { provExpanded = !provExpanded }
                ) {
                    MarketZedOutlinedTextField(
                        value = province,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Province *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = provExpanded, onDismissRequest = { provExpanded = false }) {
                        MarketZedCatalogue.provinces.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.province) },
                                onClick = {
                                    province = p.province
                                    district = MarketZedCatalogue.districtsForProvince(p.province).firstOrNull().orEmpty()
                                    town = p.towns.firstOrNull().orEmpty()
                                    provExpanded = false
                                }
                            )
                        }
                    }
                }

                val districts = MarketZedCatalogue.districtsForProvince(province)
                if (districts.isNotEmpty()) {
                    var distExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = distExpanded,
                        onExpandedChange = { distExpanded = !distExpanded }
                    ) {
                        MarketZedOutlinedTextField(
                            value = district.ifBlank { districts.first() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("District *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = distExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = distExpanded, onDismissRequest = { distExpanded = false }) {
                            districts.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        district = d
                                        distExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                val towns = MarketZedCatalogue.townsForProvince(province)
                if (towns.isNotEmpty()) {
                    var townExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = townExpanded,
                        onExpandedChange = { townExpanded = !townExpanded }
                    ) {
                        MarketZedOutlinedTextField(
                            value = town.ifBlank { towns.first() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Town *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = townExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = townExpanded, onDismissRequest = { townExpanded = false }) {
                            towns.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        town = t
                                        townExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    MarketZedOutlinedTextField(
                        value = town,
                        onValueChange = { town = it },
                        label = { Text("Town *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                MarketZedOutlinedTextField(
                    value = nearbyArea,
                    onValueChange = { nearbyArea = it },
                    label = { Text("Nearby Area / Suburb (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Availability
                Text("Availability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                WrapRow(horizontalSpacing = 8.dp, verticalSpacing = 6.dp) {
                    FilterChip(
                        selected = availabilityStatus != "unavailable",
                        onClick = { availabilityStatus = "available" },
                        label = { Text("Available") }
                    )
                    FilterChip(
                        selected = availabilityStatus == "unavailable",
                        onClick = { availabilityStatus = "unavailable" },
                        label = { Text("Unavailable") }
                    )
                }

                // Contact Preferences
                Text("Contact Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                MarketZedOutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            if (!focus.isFocused && phone.isNotBlank()) {
                                phone = PhoneValidator.autoFixZambianInput(phone)
                            }
                        }
                )
                MarketZedOutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp Number") },
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
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Public Contact Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { requestClose() },
                    enabled = !isSaving && uploadingCount == 0,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (isSaving || uploadingCount > 0) return@Button
                        val cleanTitle = title.trim()
                        val cleanDesc = description.trim()
                        if (cleanTitle.length !in 4..120) {
                            validationError = "Title must be between 4 and 120 characters."
                            return@Button
                        }
                        if (cleanDesc.length !in 20..5000) {
                            validationError = "Description must be between 20 and 5,000 characters."
                            return@Button
                        }
                        val policyErr = MarketZedCatalogue.validatePropertyAndLandPolicy(
                            categoryId = listing.categoryId,
                            subcategory = subcategory,
                            title = cleanTitle,
                            description = cleanDesc,
                            priceMode = listing.priceMode
                        )
                        if (policyErr != null) {
                            validationError = policyErr
                            return@Button
                        }
                        val parsedPrice = if (categoryKey == "free" || priceInput.isBlank()) {
                            null
                        } else {
                            PriceFormatter.parsePriceInput(priceInput) ?: run {
                                validationError = "Enter a valid non-negative ZMW amount."
                                return@Button
                            }
                        }
                        val cat = MarketZedCatalogue.categoryById(listing.categoryId)
                        if (!isWanted && cat?.requiresImage == true && images.isEmpty()) {
                            validationError = "Add at least 1 photo for ${cat.name} listings."
                            return@Button
                        }
                        val normPhone = phone.trim().takeIf { it.isNotEmpty() }?.let {
                            PhoneValidator.normalizeZambianNumber(it) ?: run {
                                validationError = "Enter a valid Zambian phone number."
                                return@Button
                            }
                        }
                        val normWa = whatsapp.trim().takeIf { it.isNotEmpty() }?.let {
                            PhoneValidator.normalizeZambianNumber(it) ?: run {
                                validationError = "Enter a valid Zambian WhatsApp number."
                                return@Button
                            }
                        }
                        val cleanEmail = email.trim().takeIf { it.isNotEmpty() }?.also {
                            if (!PhoneValidator.isValidEmail(it)) {
                                validationError = "Enter a valid email address."
                                return@Button
                            }
                        }
                        if (normPhone == null && normWa == null && cleanEmail == null) {
                            validationError = "Provide at least one contact method (Phone, WhatsApp, or Email)."
                            return@Button
                        }
                        validationError = null
                        isSaving = true

                        val changes = buildMap<String, Any?> {
                            put("subcategory", subcategory.trim())
                            put("title", cleanTitle)
                            put("description", cleanDesc)
                            put("price", parsedPrice)
                            put("availabilityStatus", availabilityStatus)
                            put(
                                "location",
                                mapOf(
                                    "province" to province.trim(),
                                    "district" to district.trim(),
                                    "town" to town.trim().ifEmpty { province.trim() },
                                    "nearbyArea" to nearbyArea.trim().takeIf { it.isNotEmpty() }
                                )
                            )
                            put(
                                "categoryDetails",
                                mapOf(
                                    "condition" to condition.trim().takeIf { it.isNotEmpty() },
                                    "bedrooms" to bedrooms.trim().takeIf { it.isNotEmpty() },
                                    "bathrooms" to bathrooms.trim().takeIf { it.isNotEmpty() },
                                    "amenities" to amenities.trim().takeIf { it.isNotEmpty() },
                                    "serviceArea" to serviceArea.trim().takeIf { it.isNotEmpty() },
                                    "employerName" to employerName.trim().takeIf { it.isNotEmpty() },
                                    "employmentType" to employmentType.trim().takeIf { it.isNotEmpty() },
                                    "swapFor" to swapFor.trim().takeIf { it.isNotEmpty() },
                                    "wantedDetails" to wantedDetails.trim().takeIf { it.isNotEmpty() }
                                )
                            )
                            put(
                                "contactPreferences",
                                mapOf(
                                    "phone" to normPhone,
                                    "whatsapp" to normWa,
                                    "email" to cleanEmail
                                )
                            )
                            if (!isWanted) {
                                put(
                                    "images",
                                    images.map { img ->
                                        mapOf(
                                            "url" to img.url,
                                            "thumbUrl" to img.thumbUrl,
                                            "publicId" to img.publicId,
                                            "width" to img.width,
                                            "height" to img.height,
                                            "format" to img.format
                                        )
                                    }
                                )
                            }
                        }

                        onSaveChanges(listing.id, changes) { ok, msg ->
                            isSaving = false
                            if (ok) {
                                onDismiss()
                            } else {
                                validationError = msg
                            }
                        }
                    },
                    enabled = !isSaving && uploadingCount == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .minimumInteractiveComponentSize()
                        .testTag("edit_save_changes_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...", color = Color.White)
                    } else {
                        Text("Save Changes", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            }
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("You have edited this listing. Closing now will discard your changes.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
