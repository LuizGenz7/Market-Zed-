package com.example.ui.screens.explore

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.PriceFormatter
import com.example.data.catalogue.MarketZedCatalogue
import com.example.data.model.Listing
import com.example.data.model.SavedSearch
import com.example.ui.components.ActiveFilterChipItem
import com.example.ui.components.ActiveFilterChipsBar
import com.example.ui.components.CategoryAwareFullWidthListingCard
import com.example.ui.components.CategorySponsoredBanner
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FullWidthListingSkeletonSection
import com.example.ui.components.LocalTopSnackbarController
import com.example.ui.components.MarketZedOutlinedTextField
import com.example.ui.components.OfflineStaleBanner
import com.example.ui.components.QuickContactListingDialog
import com.example.ui.components.WantedTextOnlySkeletonSection
import com.example.ui.screens.home.categoryIconRes
import com.example.ui.theme.MarketZedOrange
import kotlinx.coroutines.delay

private val exploreCategoryPriority = listOf(
    "marketplace",
    "property",
    "services",
    "swaps",
    "wanted",
    "jobs",
    "free"
)

private fun orderedExploreCategoryLabel(catId: String, fallbackName: String): String {
    return when (catId.lowercase()) {
        "marketplace" -> "Products"
        "property" -> "Rental Properties"
        else -> fallbackName
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onQueryChanged: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectSubcategory: (String?) -> Unit,
    onSelectProvince: (String?) -> Unit,
    onSelectSort: (String) -> Unit,
    onMinPriceChanged: (String) -> Unit,
    onMaxPriceChanged: (String) -> Unit,
    onClearFilters: () -> Unit,
    onUseRecentSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    wantedPreview: Listing? = null,
    latestJobPreview: Listing? = null,
    servicePreview: Listing? = null,
    onOpenWantedTab: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenAssistant: (() -> Unit)? = null,
    onClearBudgetFilter: () -> Unit = {
        onMinPriceChanged("")
        onMaxPriceChanged("")
    },
    comparedIds: Set<String> = emptySet(),
    onToggleCompare: (Listing) -> Unit = {},
    onOpenSeller: (Listing) -> Unit = {},
    onSelectDistrict: (String?) -> Unit = {},
    onSelectTown: (String?) -> Unit = {},
    onNearbyAreaChanged: (String) -> Unit = {},
    onSelectCondition: (String?) -> Unit = {},
    onSaveSearch: (String, Boolean) -> Unit = { _, _ -> },
    onApplySavedSearch: (SavedSearch) -> Unit = {},
    onToggleSavedSearchAlerts: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteSavedSearch: (String) -> Unit = {},
    onClearSaveSearchFeedback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFiltersPanel by remember { mutableStateOf(false) }
    var provinceMenuExpanded by remember { mutableStateOf(false) }
    var districtMenuExpanded by remember { mutableStateOf(false) }
    var townMenuExpanded by remember { mutableStateOf(false) }
    var showSaveSearchDialog by remember { mutableStateOf(false) }
    var quickContactListing by remember { mutableStateOf<Listing?>(null) }

    val orderedCategories = remember(state.categories) {
        state.categories.sortedBy { cat ->
            val idx = exploreCategoryPriority.indexOf(cat.id.lowercase())
            if (idx == -1) Int.MAX_VALUE else idx
        }
    }

    val selectedCategory = remember(state.filter.category, orderedCategories) {
        orderedCategories.firstOrNull { it.id == state.filter.category }
    }

    val availableDistricts = remember(state.filter.province) {
        MarketZedCatalogue.districtsForProvince(state.filter.province)
    }

    val availableTowns = remember(state.filter.province) {
        MarketZedCatalogue.townsForProvince(state.filter.province)
    }

    val focusRequester = remember { FocusRequester() }
    val snackbar = LocalTopSnackbarController.current
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }
    LaunchedEffect(state.errorMessage) {
        val err = state.errorMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showWarning(
            message = err,
            actionLabel = "Retry",
            onAction = onSubmitSearch
        )
    }
    LaunchedEffect(state.saveSearchFeedback) {
        val msg = state.saveSearchFeedback?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        snackbar?.showSuccess(msg)
        onClearSaveSearchFeedback()
    }

    val activeChips = remember(state.filter) {
        buildList {
            if (state.filter.query.isNotBlank()) {
                add(
                    ActiveFilterChipItem(
                        id = "query",
                        label = "\"${state.filter.query.trim()}\"",
                        onClear = { onQueryChanged("") }
                    )
                )
            }
            state.filter.category?.let { catId ->
                add(
                    ActiveFilterChipItem(
                        id = "category",
                        label = orderedExploreCategoryLabel(catId, MarketZedCatalogue.categoryLabel(catId)),
                        onClear = { onSelectCategory(null) }
                    )
                )
            }
            state.filter.subcategory?.let { sub ->
                add(
                    ActiveFilterChipItem(
                        id = "subcategory",
                        label = sub,
                        onClear = { onSelectSubcategory(null) }
                    )
                )
            }
            state.filter.province?.let { prov ->
                add(
                    ActiveFilterChipItem(
                        id = "province",
                        label = prov,
                        onClear = { onSelectProvince(null) }
                    )
                )
            }
            state.filter.district?.let { district ->
                add(
                    ActiveFilterChipItem(
                        id = "district",
                        label = "$district District",
                        onClear = { onSelectDistrict(null) }
                    )
                )
            }
            state.filter.town?.let { town ->
                add(
                    ActiveFilterChipItem(
                        id = "town",
                        label = town,
                        onClear = { onSelectTown(null) }
                    )
                )
            }
            state.filter.nearbyArea?.takeIf { it.isNotBlank() }?.let { area ->
                add(
                    ActiveFilterChipItem(
                        id = "nearby_area",
                        label = "Near: $area",
                        onClear = { onNearbyAreaChanged("") }
                    )
                )
            }
            state.filter.condition?.let { cond ->
                add(
                    ActiveFilterChipItem(
                        id = "condition",
                        label = cond,
                        onClear = { onSelectCondition(null) }
                    )
                )
            }
            if (state.filter.minPrice != null || state.filter.maxPrice != null) {
                val minLabel = state.filter.minPrice?.let { PriceFormatter.formatKwachaAmount(it) } ?: "K0"
                val maxLabel = state.filter.maxPrice?.let { PriceFormatter.formatKwachaAmount(it) } ?: "Any"
                add(
                    ActiveFilterChipItem(
                        id = "budget",
                        label = "$minLabel – $maxLabel",
                        onClear = onClearBudgetFilter
                    )
                )
            }
            if (state.filter.sort == "oldest") {
                add(
                    ActiveFilterChipItem(
                        id = "sort",
                        label = "Oldest first",
                        onClear = { onSelectSort("newest") }
                    )
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header with Reset
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (activeChips.isNotEmpty() || state.hasSearched) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag("explore_reset_button")
                    ) {
                        Text("Reset", color = MarketZedOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Compact Search input + aligned 48dp Filter toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarketZedOutlinedTextField(
                    value = state.filter.query,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search products, properties, jobs...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_search),
                            contentDescription = null,
                            tint = MarketZedOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (state.filter.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_x),
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("explore_search_input")
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (showFiltersPanel) MarketZedOrange else MarketZedOrange,
                    border = BorderStroke(
                        1.dp,
                        if (state.budgetError != null || showFiltersPanel) MarketZedOrange else MarketZedOrange
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("explore_filters_toggle")
                        .clickable { showFiltersPanel = !showFiltersPanel }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_sliders_horizontal),
                            contentDescription = "Toggle filters",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (state.filter.query.trim().contains(" ")) {
            item {
                Text(
                    text = "Tip: MarketZed search works best with one keyword plus category or province filters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Priority Category Pills (All + 7 categories)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.filter.category == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MarketZedOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(orderedCategories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = state.filter.category == cat.id,
                        onClick = { onSelectCategory(cat.id) },
                        label = { Text(orderedExploreCategoryLabel(cat.id, cat.name)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MarketZedOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("category_chip_${cat.id}")
                    )
                }
            }
        }

        // Subcategories for selected category
        if (selectedCategory != null && selectedCategory.subcategories.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.filter.subcategory == null,
                            onClick = { onSelectSubcategory(null) },
                            label = { Text("All ${orderedExploreCategoryLabel(selectedCategory.id, selectedCategory.name)}") }
                        )
                    }
                    items(selectedCategory.subcategories) { sub ->
                        FilterChip(
                            selected = state.filter.subcategory == sub,
                            onClick = { onSelectSubcategory(sub) },
                            label = { Text(sub) }
                        )
                    }
                }
            }
        }

        // Active filter chips + Save Search button
        if (activeChips.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActiveFilterChipsBar(
                        items = activeChips,
                        onClearAll = onClearFilters
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showSaveSearchDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("explore_save_search_button")
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_bookmark),
                                contentDescription = null,
                                tint = MarketZedOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Search", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Saved Searches Strip
        if (state.savedSearches.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Saved Searches",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.savedSearches, key = { "saved_search_${it.id}" }) { saved ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                modifier = Modifier.clickable { onApplySavedSearch(saved) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = saved.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { onToggleSavedSearchAlerts(saved.id, !saved.notifyMatches) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (saved.notifyMatches) LucideR.drawable.lucide_ic_bell_ring
                                                else LucideR.drawable.lucide_ic_bell
                                            ),
                                            contentDescription = "Toggle alerts for ${saved.name}",
                                            tint = if (saved.notifyMatches) MarketZedOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteSavedSearch(saved.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(LucideR.drawable.lucide_ic_x),
                                            contentDescription = "Delete ${saved.name}",
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

        // Recent searches
        if (state.searchHistory.isNotEmpty() && !state.hasSearched) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onClearRecentSearches) {
                            Text("Clear", color = MarketZedOrange)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.searchHistory) { item ->
                            FilterChip(
                                selected = false,
                                onClick = { onUseRecentSearch(item) },
                                label = { Text(item) },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_x),
                                        contentDescription = "Remove $item",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemoveRecentSearch(item) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Expanded Filter Panel (Province, Town, Budget, Condition, Sort)
        if (showFiltersPanel) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = provinceMenuExpanded,
                            onExpandedChange = { provinceMenuExpanded = it }
                        ) {
                            MarketZedOutlinedTextField(
                                value = state.filter.province ?: "All Provinces",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Province") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceMenuExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = provinceMenuExpanded,
                                onDismissRequest = { provinceMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Provinces") },
                                    onClick = {
                                        onSelectProvince(null)
                                        provinceMenuExpanded = false
                                    }
                                )
                                state.provinces.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov.province) },
                                        onClick = {
                                            onSelectProvince(prov.province)
                                            provinceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (availableDistricts.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = districtMenuExpanded,
                                onExpandedChange = { districtMenuExpanded = it }
                            ) {
                                MarketZedOutlinedTextField(
                                    value = state.filter.district ?: "All Districts",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("District") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(LucideR.drawable.lucide_ic_building),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtMenuExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("explore_district_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = districtMenuExpanded,
                                    onDismissRequest = { districtMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Districts") },
                                        onClick = {
                                            onSelectDistrict(null)
                                            districtMenuExpanded = false
                                        }
                                    )
                                    availableDistricts.forEach { dist ->
                                        DropdownMenuItem(
                                            text = { Text(dist) },
                                            onClick = {
                                                onSelectDistrict(dist)
                                                districtMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (availableTowns.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = townMenuExpanded,
                                onExpandedChange = { townMenuExpanded = it }
                            ) {
                                MarketZedOutlinedTextField(
                                    value = state.filter.town ?: "All Towns in ${state.filter.province}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Town") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(LucideR.drawable.lucide_ic_map_pinned),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = townMenuExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("explore_town_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = townMenuExpanded,
                                    onDismissRequest = { townMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Towns") },
                                        onClick = {
                                            onSelectTown(null)
                                            townMenuExpanded = false
                                        }
                                    )
                                    availableTowns.forEach { town ->
                                        DropdownMenuItem(
                                            text = { Text(town) },
                                            onClick = {
                                                onSelectTown(town)
                                                townMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MarketZedOutlinedTextField(
                            value = state.filter.nearbyArea.orEmpty(),
                            onValueChange = onNearbyAreaChanged,
                            label = { Text("Nearby area / neighbourhood (optional)") },
                            placeholder = { Text("e.g. Kabulonga, Chalala, Riverside") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideR.drawable.lucide_ic_map_pin),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("explore_nearby_area_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MarketZedOutlinedTextField(
                                value = state.minPriceText,
                                onValueChange = onMinPriceChanged,
                                label = { Text("Min ZMW (K)") },
                                isError = state.budgetError != null,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_banknote),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("explore_min_price_input")
                            )
                            MarketZedOutlinedTextField(
                                value = state.maxPriceText,
                                onValueChange = onMaxPriceChanged,
                                label = { Text("Max ZMW (K)") },
                                isError = state.budgetError != null,
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(LucideR.drawable.lucide_ic_banknote),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("explore_max_price_input")
                            )
                        }

                        if (!state.budgetError.isNullOrBlank()) {
                            Text(
                                text = state.budgetError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("explore_budget_error")
                            )
                        } else if (state.filter.minPrice != null || state.filter.maxPrice != null) {
                            Text(
                                text = "Unpriced listings are excluded when a ZMW budget filter is active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (state.filter.category == null || state.filter.category in setOf("marketplace", "swaps", "free")) {
                            Text("Condition", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = state.filter.condition == null,
                                        onClick = { onSelectCondition(null) },
                                        label = { Text("Any") }
                                    )
                                }
                                items(MarketZedCatalogue.conditionOptions) { cond ->
                                    FilterChip(
                                        selected = state.filter.condition == cond,
                                        onClick = { onSelectCondition(cond) },
                                        label = { Text(cond) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = state.filter.sort == "newest",
                                    onClick = { onSelectSort("newest") },
                                    label = { Text("Newest") }
                                )
                                FilterChip(
                                    selected = state.filter.sort == "oldest",
                                    onClick = { onSelectSort("oldest") },
                                    label = { Text("Oldest") }
                                )
                            }
                            TextButton(onClick = onClearFilters) {
                                Text("Clear all")
                            }
                        }

                        Button(
                            onClick = {
                                onSubmitSearch()
                                if (state.budgetError == null) {
                                    showFiltersPanel = false
                                }
                            },
                            enabled = state.budgetError == null,
                            colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("explore_apply_filters_button")
                        ) {
                            Text("Apply Filters")
                        }
                    }
                }
            }
        }

        // Directory before searching vs Search results
        if (!state.hasSearched) {
            // Directory before searching
            item {
                Text(
                    text = "All Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(orderedCategories, key = { "dir_${it.id}" }) { cat ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCategory(cat.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(categoryIconRes(cat.id)),
                            contentDescription = null,
                            tint = MarketZedOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = orderedExploreCategoryLabel(cat.id, cat.name),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = cat.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (wantedPreview != null || latestJobPreview != null || servicePreview != null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Explore Across Zambia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                wantedPreview?.let { wanted ->
                    item(key = "explore_preview_wanted_${wanted.id}") {
                        SecondaryPreviewRow(
                            sectionLabel = "Wanted Request",
                            actionLabel = "View all",
                            onAction = onOpenWantedTab,
                            listing = wanted,
                            isFavorite = wanted.id in state.favoriteIds,
                            isCompared = wanted.id in comparedIds,
                            dataSaverEnabled = state.settings.dataSaverEnabled,
                            onOpenListing = onOpenListing,
                            onToggleFavorite = onToggleFavorite,
                            onToggleCompare = onToggleCompare,
                            onContactClick = { quickContactListing = wanted },
                            onSellerClick = { onOpenSeller(wanted) }
                        )
                    }
                }
                latestJobPreview?.let { job ->
                    item(key = "explore_preview_job_${job.id}") {
                        SecondaryPreviewRow(
                            sectionLabel = "Latest Job",
                            actionLabel = "View all",
                            onAction = { onSelectCategory("jobs") },
                            listing = job,
                            isFavorite = job.id in state.favoriteIds,
                            isCompared = job.id in comparedIds,
                            dataSaverEnabled = state.settings.dataSaverEnabled,
                            onOpenListing = onOpenListing,
                            onToggleFavorite = onToggleFavorite,
                            onToggleCompare = onToggleCompare,
                            onContactClick = { quickContactListing = job },
                            onSellerClick = { onOpenSeller(job) }
                        )
                    }
                }
                servicePreview?.let { service ->
                    item(key = "explore_preview_service_${service.id}") {
                        SecondaryPreviewRow(
                            sectionLabel = "Local Service",
                            actionLabel = "View all",
                            onAction = { onSelectCategory("services") },
                            listing = service,
                            isFavorite = service.id in state.favoriteIds,
                            isCompared = service.id in comparedIds,
                            dataSaverEnabled = state.settings.dataSaverEnabled,
                            onOpenListing = onOpenListing,
                            onToggleFavorite = onToggleFavorite,
                            onToggleCompare = onToggleCompare,
                            onContactClick = { quickContactListing = service },
                            onSellerClick = { onOpenSeller(service) }
                        )
                    }
                }
            }
        } else {
            if (state.isOfflineFallback) {
                item {
                    OfflineStaleBanner(
                        isOffline = true,
                        isStale = true,
                        lastSyncedAt = state.lastSyncedAt,
                        onRetrySync = onSubmitSearch
                    )
                }
            }

            // Category top sponsored banner (excluded for Wanted text-only view)
            if (state.sponsoredHighlights.isNotEmpty() && !state.filter.category.equals("wanted", ignoreCase = true)) {
                item {
                    CategorySponsoredBanner(
                        categoryLabel = selectedCategory?.let { orderedExploreCategoryLabel(it.id, it.name) } ?: "MarketZed",
                        listings = state.sponsoredHighlights,
                        dataSaverEnabled = state.settings.dataSaverEnabled,
                        onOpenListing = onOpenListing
                    )
                }
            }

            if (state.isLoading) {
                item {
                    if (state.filter.category.equals("wanted", ignoreCase = true)) {
                        WantedTextOnlySkeletonSection(count = 4)
                    } else {
                        FullWidthListingSkeletonSection(count = 4)
                    }
                }
            } else if (state.results.isEmpty()) {
                item {
                    EmptyStateView(
                        iconRes = LucideR.drawable.lucide_ic_search,
                        title = "No matching listings",
                        message = "Try adjusting your search or removing some filters.",
                        actionLabel = "Clear Filters",
                        onAction = onClearFilters
                    )
                }
            } else {
                item {
                    Text(
                        text = "${state.results.size} ${if (state.results.size == 1) "listing" else "listings"} shown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // ALL Explore results render full-width 100% single-column via CategoryAwareFullWidthListingCard
                items(state.results, key = { "explore_item_${it.id}" }) { listing ->
                    CategoryAwareFullWidthListingCard(
                        listing = listing,
                        isFavorite = listing.id in state.favoriteIds,
                        isCompared = listing.id in comparedIds,
                        dataSaverEnabled = state.settings.dataSaverEnabled,
                        onClick = { onOpenListing(listing.id) },
                        onToggleFavorite = { onToggleFavorite(listing.id) },
                        onToggleCompare = if (listing.isComparableCategory) {
                            { onToggleCompare(listing) }
                        } else null,
                        onContactClick = { quickContactListing = listing },
                        onSellerClick = { onOpenSeller(listing) }
                    )
                }
                if (state.isLoadingMore) {
                    item {
                        if (state.filter.category.equals("wanted", ignoreCase = true)) {
                            WantedTextOnlySkeletonSection(count = 2)
                        } else {
                            FullWidthListingSkeletonSection(count = 2)
                        }
                    }
                } else if (state.nextCursor != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = onLoadNextPage,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Load more listings")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    quickContactListing?.let { target ->
        QuickContactListingDialog(
            listing = target,
            onDismiss = { quickContactListing = null },
            onViewDetails = { onOpenListing(target.id) },
            onOpenSellerProfile = { onOpenSeller(target) }
        )
    }

    if (showSaveSearchDialog) {
        val suggestedName = buildString {
            if (state.filter.query.isNotBlank()) append(state.filter.query.trim())
            state.filter.category?.let {
                if (isNotEmpty()) append(" · ")
                append(orderedExploreCategoryLabel(it, MarketZedCatalogue.categoryLabel(it)))
            }
            state.filter.province?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
        }.ifBlank { "My MarketZed Search" }

        var searchName by remember(suggestedName) { mutableStateOf(suggestedName) }
        var notifyMatches by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showSaveSearchDialog = false },
            title = { Text("Save Search") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MarketZedOutlinedTextField(
                        value = searchName,
                        onValueChange = { if (it.length <= 60) searchName = it },
                        label = { Text("Search name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Match alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Text("Highlight new local matches during sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notifyMatches,
                            onCheckedChange = { notifyMatches = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MarketZedOrange)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveSearch(searchName, notifyMatches)
                        showSaveSearchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveSearchDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SecondaryPreviewRow(
    sectionLabel: String,
    actionLabel: String,
    onAction: () -> Unit,
    listing: Listing,
    isFavorite: Boolean,
    isCompared: Boolean,
    dataSaverEnabled: Boolean,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleCompare: (Listing) -> Unit,
    onContactClick: () -> Unit,
    onSellerClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MarketZedOrange, fontWeight = FontWeight.SemiBold)
            }
        }
        CategoryAwareFullWidthListingCard(
            listing = listing,
            isFavorite = isFavorite,
            isCompared = isCompared,
            dataSaverEnabled = dataSaverEnabled,
            onClick = { onOpenListing(listing.id) },
            onToggleFavorite = { onToggleFavorite(listing.id) },
            onToggleCompare = if (listing.isComparableCategory) {
                { onToggleCompare(listing) }
            } else null,
            onContactClick = onContactClick,
            onSellerClick = onSellerClick
        )
    }
}
