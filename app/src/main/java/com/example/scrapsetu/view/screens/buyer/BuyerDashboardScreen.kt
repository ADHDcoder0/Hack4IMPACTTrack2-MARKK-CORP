package com.example.scrapsetu.view.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.User
import com.example.scrapsetu.ui.theme.EcoDeepForest
import com.example.scrapsetu.ui.theme.EcoInteractionWhite
import com.example.scrapsetu.ui.theme.EcoOnSurface
import com.example.scrapsetu.ui.theme.EcoOnSurfaceVariant
import com.example.scrapsetu.ui.theme.EcoSageGrowth
import com.example.scrapsetu.ui.theme.EcoSectionMint
import com.example.scrapsetu.view.components.AnalyticsShell
import com.example.scrapsetu.view.components.DemandForecastChip
import com.example.scrapsetu.vm.AnalyticsUiState
import com.example.scrapsetu.vm.AuthViewModel
import com.example.scrapsetu.vm.GroqState
import com.example.scrapsetu.vm.GroqAnalyticsViewModel
import com.example.scrapsetu.vm.GroqViewModel
import com.example.scrapsetu.vm.ListingState
import com.example.scrapsetu.vm.ListingViewModel
import com.example.scrapsetu.vm.MatchState
import com.example.scrapsetu.vm.MatchViewModel
import com.example.scrapsetu.vm.SmartMatchInsightState
import com.example.scrapsetu.vm.SmartMatchInsightViewModel
import com.example.scrapsetu.vm.UserViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BuyerDashboardScreen(
    onSignOut: () -> Unit,
    onViewMatches: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfile: () -> Unit,
    listingViewModel: ListingViewModel = hiltViewModel(),
    matchViewModel: MatchViewModel = hiltViewModel(),
    groqViewModel: GroqViewModel = hiltViewModel(),
    smartMatchInsightViewModel: SmartMatchInsightViewModel = hiltViewModel(),
    analyticsViewModel: GroqAnalyticsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val groqState by groqViewModel.groqState.collectAsState()
    val insightState by smartMatchInsightViewModel.insightState.collectAsState()
    val analyticsState by analyticsViewModel.state.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var selectedListing by remember { mutableStateOf<Listing?>(null) }
    val listings by listingViewModel.listings.collectAsState()
    val vmFilteredListings by listingViewModel.filteredListings.collectAsState()
    val selectedMaterialFilter by listingViewModel.selectedMaterialFilter.collectAsState()
    val selectedStateFilter by listingViewModel.selectedStateFilter.collectAsState()
    val usersById by userViewModel.usersById.collectAsState()
    val uiState by listingViewModel.uiState.collectAsState()
    val matchState by matchViewModel.matchState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    val requestedMatchIds = remember { mutableStateListOf<String>() }
    var pendingMatchRequestId by remember { mutableStateOf<String?>(null) }
    var draftMaterial by remember { mutableStateOf<String?>(null) }
    var draftState by remember { mutableStateOf<String?>(null) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val isRefreshing = uiState is ListingState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { listingViewModel.loadActiveListings(forceRefresh = true) }
    )

    LaunchedEffect(Unit) { listingViewModel.loadActiveListings() }
    LaunchedEffect(Unit) { authViewModel.loadCurrentUserDetails() }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { analyticsViewModel.load(userId = it) }
    }

    LaunchedEffect(vmFilteredListings) {
        userViewModel.loadUsersByIds(vmFilteredListings.map { it.supplierId })
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(matchState) {
        when (matchState) {
            is MatchState.Success -> {
                pendingMatchRequestId?.let { requestedId ->
                    if (!requestedMatchIds.contains(requestedId)) {
                        requestedMatchIds.add(requestedId)
                    }
                }
                pendingMatchRequestId = null
                snackbarHostState.showSnackbar("Match requested!")
            }

            is MatchState.Error -> {
                pendingMatchRequestId = null
                snackbarHostState.showSnackbar((matchState as MatchState.Error).message)
            }

            else -> Unit
        }
    }

    selectedListing?.let { listing ->
        SmartMatchDialog(
            selectedListing = listing,
            seller = usersById[listing.supplierId],
            groqState = groqState,
            onDismiss = {
                selectedListing = null
                groqViewModel.reset()
                smartMatchInsightViewModel.reset()
            },
            onRequest = {
                listing.id?.let { id ->
                    matchViewModel.requestMatch(id)
                }
                selectedListing = null
                groqViewModel.reset()
                smartMatchInsightViewModel.reset()
            },
            insightState = insightState,
            analyticsState = analyticsState
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ScrapSetu",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onSignOut) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            }
        },
        bottomBar = {
            BuyerBottomBar(
                onViewMatches = onViewMatches,
                onOpenDashboard = onOpenDashboard,
                onOpenProfile = onOpenProfile
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val filtered = vmFilteredListings.filter {
            it.wasteType.contains(searchQuery, ignoreCase = true) ||
                    it.location.contains(searchQuery, ignoreCase = true)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                uiState is ListingState.Loading && listings.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            BuyerIntroHeader()
                        }

                        item {
                            SearchAndFilterRow(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onFilterClick = {
                                    draftMaterial = selectedMaterialFilter
                                    draftState = selectedStateFilter
                                    showFilterSheet = true
                                }
                            )
                        }

                        if (!selectedMaterialFilter.isNullOrBlank() || !selectedStateFilter.isNullOrBlank()) {
                            item {
                                ActiveFiltersRow(
                                    selectedMaterial = selectedMaterialFilter,
                                    selectedState = selectedStateFilter,
                                    onClear = { listingViewModel.clearFilters() }
                                )
                            }
                        }

                        item {
                            MarketTrendCard(
                                listings = filtered,
                                analyticsState = analyticsState
                            )
                        }

                        item {
                            AnalyticsShell(state = analyticsState) { data ->
                                DemandForecastChip(forecast = data.demandForecast)
                            }
                        }

                        item {
                            ListingsHeader(onViewAllClick = onViewMatches)
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    text = "No listings found.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            itemsIndexed(filtered) { index, listing ->
                                val isMatched = listing.status.equals("matched", ignoreCase = true) ||
                                    listing.status.equals("closed", ignoreCase = true)
                                val isRequested = listing.id != null && requestedMatchIds.contains(listing.id)
                                BuyerListingCard(
                                    listing = listing,
                                    seller = usersById[listing.supplierId],
                                    tag = listingTag(index),
                                    isMatched = isMatched,
                                    isRequested = isRequested,
                                    onRequestMatch = {
                                        if (!isMatched && !isRequested) {
                                            listing.id?.let { id ->
                                                pendingMatchRequestId = id
                                                if (!requestedMatchIds.contains(id)) {
                                                    requestedMatchIds.add(id)
                                                }
                                                matchViewModel.requestMatch(id)
                                            }
                                        }
                                    },
                                    onSmartMatch = {
                                        if (!isMatched) {
                                            selectedListing = listing
                                            groqViewModel.getSuggestion(listing)
                                            smartMatchInsightViewModel.loadLatestInsight(listing.id)
                                            currentUser?.id?.let { userId ->
                                                analyticsViewModel.load(userId = userId, listingId = listing.id)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState
        ) {
            BuyerFilterSheet(
                selectedMaterial = draftMaterial,
                selectedState = draftState,
                onMaterialSelected = { draftMaterial = it },
                onStateSelected = { draftState = it },
                onApply = {
                    listingViewModel.applyFilters(draftMaterial, draftState)
                    scope.launch {
                        filterSheetState.hide()
                        showFilterSheet = false
                    }
                },
                onClear = {
                    draftMaterial = null
                    draftState = null
                    listingViewModel.clearFilters()
                    scope.launch {
                        filterSheetState.hide()
                        showFilterSheet = false
                    }
                }
            )
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    selectedMaterial: String?,
    selectedState: String?,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!selectedMaterial.isNullOrBlank()) {
            AssistChip(onClick = {}, label = { Text("Material: $selectedMaterial") })
        }
        if (!selectedState.isNullOrBlank()) {
            AssistChip(onClick = {}, label = { Text("State: $selectedState") })
        }
        TextButton(onClick = onClear) {
            Text("Clear")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun BuyerFilterSheet(
    selectedMaterial: String?,
    selectedState: String?,
    onMaterialSelected: (String?) -> Unit,
    onStateSelected: (String?) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    val materials = listOf("Plastic", "Metal", "Textile", "Paper", "Chemical", "E-Waste", "Other")
    val states = listOf("Maharashtra", "Gujarat", "Karnataka", "Tamil Nadu", "Delhi", "Rajasthan", "Uttar Pradesh", "Madhya Pradesh", "Telangana", "West Bengal")
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("Material Type", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            materials.forEach { material ->
                FilterChip(
                    selected = selectedMaterial == material,
                    onClick = { onMaterialSelected(if (selectedMaterial == material) null else material) },
                    label = { Text(material) }
                )
            }
        }

        Text("State", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedState.orEmpty(),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select state") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                states.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state) },
                        onClick = {
                            onStateSelected(state)
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear") }
            Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Apply") }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun BuyerListingCard(
    listing: Listing,
    seller: User?,
    tag: ListingTag,
    isMatched: Boolean,
    isRequested: Boolean,
    onRequestMatch: () -> Unit,
    onSmartMatch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .alpha(if (isMatched) 0.5f else 1f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = listing.imageUrl.takeIf { it.isNotBlank() },
                    contentDescription = "Listing photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                )

                Surface(
                    color = tag.container,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(
                        text = tag.label,
                        color = tag.content,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = listing.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = listing.wasteType,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listing.description.ifBlank { "Verified industrial material ready for recycling and processing." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Seller: ${seller?.name?.ifBlank { "Unknown" } ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip(
                        label = "Quantity",
                        value = "${listing.quantityKg} kg",
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "Unit Price",
                        value = "₹${listing.pricePerKg}/kg",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isMatched) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFD8F3DC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Matched",
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (isRequested) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = EcoSectionMint,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Match Requested",
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = EcoDeepForest,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Button(
                        onClick = onRequestMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Request Match", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onSmartMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Smart Match")
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyerIntroHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Buyer Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Welcome back, Curator. Here are your active material flows.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
    }
}

@Composable
private fun SearchAndFilterRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search by waste type, material...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun MarketTrendCard(
    listings: List<Listing>,
    analyticsState: AnalyticsUiState
) {
    val totalValue = listings.sumOf { it.quantityKg * it.pricePerKg }
    val totalQuantity = listings.sumOf { it.quantityKg }
    val avgPrice = listings.map { it.pricePerKg }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    val dominantMaterial = listings
        .groupingBy { it.wasteType }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?.takeIf { it.isNotBlank() }
        ?: "Mixed"
    val forecast = (analyticsState as? AnalyticsUiState.Success)?.data?.demandForecast
    val trendLabel = forecast?.trend?.replaceFirstChar { it.uppercase() } ?: "Stable"
    val trendInsight = forecast?.insight?.takeIf { it.isNotBlank() }
        ?: "$dominantMaterial demand is tracking stable across active market listings."

    if (totalValue <= 0.0 || totalQuantity <= 0.0) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EcoDeepForest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EcoInteractionWhite.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "AI MARKET PULSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = EcoInteractionWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$trendLabel Momentum\nfor $dominantMaterial",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = EcoInteractionWhite
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = trendInsight,
                color = EcoInteractionWhite.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${"%.0f".format(totalValue)}",
                        color = EcoInteractionWhite,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("ACTIVE VALUE", color = EcoInteractionWhite.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(EcoInteractionWhite.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${"%.0f".format(totalQuantity)} kg",
                        color = EcoInteractionWhite,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("OPEN INVENTORY", color = EcoInteractionWhite.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EcoSectionMint.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Avg Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = EcoInteractionWhite.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "₹${"%.0f".format(avgPrice)}/kg",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EcoInteractionWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingsHeader(onViewAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "New Opportunities",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Fresh supplier opportunities curated for your marketplace flow",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        TextButton(onClick = onViewAllClick) {
            Text("View All")
            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SmartMatchDialog(
    selectedListing: Listing,
    seller: User?,
    groqState: GroqState,
    insightState: SmartMatchInsightState,
    analyticsState: AnalyticsUiState,
    onDismiss: () -> Unit,
    onRequest: () -> Unit
) {
    val analytics = (analyticsState as? AnalyticsUiState.Success)?.data
    val smartMatch = analytics?.smartMatch
    val demandForecast = analytics?.demandForecast
    val buyerSuggestions = analytics?.buyerSuggestions
    val priceSuggestion = analytics?.priceSuggestion

    val requirementTitle = selectedListing.wasteType.ifBlank { "Material Requirement" }
    val requirementVolume = if (selectedListing.quantityKg > 0) {
        "${"%.1f".format(selectedListing.quantityKg)} kg"
    } else {
        "Not specified"
    }
    val requirementLocation = selectedListing.location.ifBlank { "Location not provided" }
    val supplierName = seller?.name?.takeIf { it.isNotBlank() } ?: "Verified Supplier"
    val etaText = smartMatch?.estimatedEta ?: etaLabel(insightState)
    val confidenceText = smartMatch?.confidence?.replaceFirstChar { it.uppercase() } ?: "Medium"

    val marketReferencePrice = priceSuggestion?.let { (it.minPriceInr + it.maxPriceInr) / 2.0 }
    val efficiencyText = if (selectedListing.pricePerKg > 0.0 && marketReferencePrice != null && marketReferencePrice > 0) {
        val delta = (((selectedListing.pricePerKg - marketReferencePrice) / marketReferencePrice) * 100.0).roundToInt()
        if (delta >= 0) "+$delta% vs market" else "$delta% vs market"
    } else {
        "Market baseline pending"
    }

    val logicLineOne = smartMatch?.reason
        ?: "Match quality is ranked from live availability and fulfillment history."
    val logicLineTwo = demandForecast?.insight
        ?: "Demand momentum is derived from current listing activity in your selected flow."
    val logicLineThree = buyerSuggestions?.summary
        ?.takeIf { it.isNotBlank() }
        ?: confidenceNote(insightState)
        ?: "Confidence updates as more buyer-supplier interactions are confirmed."

    val reliabilityText = smartMatch?.reliabilityScore?.let { "$it%" } ?: reliabilityLabel(insightState)
    val reliabilityProgress = (reliabilityText.removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0.84f)
        .coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EcoDeepForest.copy(alpha = 0.24f))
                .padding(horizontal = 12.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EcoInteractionWhite.copy(alpha = 0.86f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(0.88f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        EcoDeepForest,
                                        Color(0xFF1B4332)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.White.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "AI RECOMMENDATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EcoInteractionWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = EcoInteractionWhite
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Smart Match\nReady",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = EcoInteractionWhite,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = logicLineOne,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EcoInteractionWhite.copy(alpha = 0.76f)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RELIABILITY SCORE",
                            style = MaterialTheme.typography.titleMedium,
                            color = EcoOnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reliabilityText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = EcoSageGrowth,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { reliabilityProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = EcoSageGrowth,
                        trackColor = EcoSectionMint
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = EcoSectionMint,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text(
                                    text = "YOUR REQUIREMENT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EcoOnSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = requirementTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EcoOnSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Material", color = EcoOnSurfaceVariant)
                                    Text(requirementTitle, fontWeight = FontWeight.Bold, color = EcoOnSurface)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Volume", color = EcoOnSurfaceVariant)
                                    Text(requirementVolume, fontWeight = FontWeight.Bold, color = EcoOnSurface)
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = EcoSectionMint.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                    Text(
                                        text = "PERFECT MATCH",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EcoSageGrowth,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = supplierName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EcoOnSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("ETA", color = EcoOnSurfaceVariant)
                                        Text(etaText, fontWeight = FontWeight.Bold, color = EcoOnSurface)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Market Fit", color = EcoOnSurfaceVariant)
                                        Text(efficiencyText, fontWeight = FontWeight.Bold, color = EcoOnSurface)
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = EcoInteractionWhite,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-8).dp)
                            ) {
                                Icon(
                                    Icons.Filled.Handshake,
                                    contentDescription = null,
                                    tint = EcoSageGrowth,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = EcoInteractionWhite.copy(alpha = 0.92f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (groqState) {
                                is GroqState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                is GroqState.Success -> {
                                    Text(
                                        text = groqState.suggestion,
                                        color = EcoOnSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                is GroqState.Error -> {
                                    Text(
                                        text = groqState.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                else -> Unit
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "MATCHING LOGIC",
                                style = MaterialTheme.typography.labelSmall,
                                color = EcoOnSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = EcoSageGrowth, modifier = Modifier.size(16.dp))
                                Text(
                                    text = logicLineOne,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoOnSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = EcoSageGrowth, modifier = Modifier.size(16.dp))
                                Text(
                                    text = logicLineTwo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoOnSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.History, contentDescription = null, tint = EcoSageGrowth, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "$logicLineThree ($confidenceText confidence)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoOnSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EcoSectionMint.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Location", style = MaterialTheme.typography.labelMedium, color = EcoOnSurfaceVariant)
                                    Text(requirementLocation, style = MaterialTheme.typography.bodyMedium, color = EcoOnSurface, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(EcoDeepForest, Color(0xFF1B4332))),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Request Match", fontWeight = FontWeight.Bold, color = EcoInteractionWhite)
                    }
                }
            }
        }
    }
}

}

@Composable
private fun BuyerBottomBar(
    onViewMatches: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(icon = Icons.Filled.Dashboard, label = "Dashboard", selected = true, onClick = onOpenDashboard)
            BottomNavItem(icon = Icons.Filled.VolunteerActivism, label = "Matches", selected = false, onClick = onViewMatches)
            BottomNavItem(icon = Icons.Filled.Person, label = "Profile", selected = false, onClick = onOpenProfile)
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class ListingTag(
    val label: String,
    val container: Color,
    val content: Color
)

@Composable
private fun listingTag(index: Int): ListingTag {
    return when (index % 3) {
        0 -> ListingTag(
            label = "PENDING MATCH",
            container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
            content = MaterialTheme.colorScheme.tertiary
        )

        1 -> ListingTag(
            label = "DIRECT LISTING",
            container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
            content = MaterialTheme.colorScheme.onSecondary
        )

        else -> ListingTag(
            label = "EXPIRING SOON",
            container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
            content = MaterialTheme.colorScheme.tertiary
        )
    }
}

private fun reliabilityLabel(insightState: SmartMatchInsightState): String {
    val score = (insightState as? SmartMatchInsightState.Success)?.insight?.reliabilityScore
    return if (score != null) "${score.toInt()}%" else "Pending"
}

private fun etaLabel(insightState: SmartMatchInsightState): String {
    val etaDays = (insightState as? SmartMatchInsightState.Success)?.insight?.etaDays
    return if (etaDays != null && etaDays > 0) "$etaDays days" else "N/A"
}

private fun confidenceNote(insightState: SmartMatchInsightState): String? {
    return when (insightState) {
        is SmartMatchInsightState.Success -> {
            insightState.insight.confidenceNote?.takeIf { it.isNotBlank() }
        }

        SmartMatchInsightState.Empty -> "No persisted smart insight found yet for this listing."
        is SmartMatchInsightState.Error -> insightState.message
        else -> null
    }
}