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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.User
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
    val usersById by userViewModel.usersById.collectAsState()
    val uiState by listingViewModel.uiState.collectAsState()
    val matchState by matchViewModel.matchState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

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

    LaunchedEffect(listings) {
        userViewModel.loadUsersByIds(listings.map { it.supplierId })
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(matchState) {
        when (matchState) {
            is MatchState.Success -> snackbarHostState.showSnackbar("Match requested!")
            is MatchState.Error -> snackbarHostState.showSnackbar((matchState as MatchState.Error).message)
            else -> Unit
        }
    }

    if (selectedListing != null) {
        SmartMatchDialog(
            groqState = groqState,
            onDismiss = {
                selectedListing = null
                groqViewModel.reset()
                smartMatchInsightViewModel.reset()
            },
            onRequest = {
                selectedListing?.id?.let { id ->
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
        val filtered = listings.filter {
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
                                onFilterClick = onViewMatches
                            )
                        }

                        item {
                            MarketTrendCard(listings = filtered)
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
                                BuyerListingCard(
                                    listing = listing,
                                    seller = usersById[listing.supplierId],
                                    tag = listingTag(index),
                                    onRequestMatch = {
                                        listing.id?.let { id ->
                                            matchViewModel.requestMatch(id)
                                        }
                                    },
                                    onSmartMatch = {
                                        selectedListing = listing
                                        groqViewModel.getSuggestion(listing)
                                        smartMatchInsightViewModel.loadLatestInsight(listing.id)
                                        currentUser?.id?.let { userId ->
                                            analyticsViewModel.load(userId = userId, listingId = listing.id)
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
}

@Composable
private fun BuyerListingCard(
    listing: Listing,
    seller: User?,
    tag: ListingTag,
    onRequestMatch: () -> Unit,
    onSmartMatch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box {
                if (listing.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = "Waste image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

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
private fun MarketTrendCard(listings: List<Listing>) {
    val totalValue = listings.sumOf { it.quantityKg * it.pricePerKg }
    val totalQuantity = listings.sumOf { it.quantityKg }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "MARKET TRENDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Sustainable\nSourcing Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Live metrics from current active listings in your feed.",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "₹${"%.0f".format(totalValue)}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("ACTIVE VALUE", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${"%.0f".format(totalQuantity)} kg",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("OPEN INVENTORY", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
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
    groqState: GroqState,
    insightState: SmartMatchInsightState,
    analyticsState: AnalyticsUiState,
    onDismiss: () -> Unit,
    onRequest: () -> Unit
) {
    val smartMatch = (analyticsState as? AnalyticsUiState.Success)?.data?.smartMatch
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
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f))
                .padding(horizontal = 12.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(0.92f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(188.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
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
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Smart Match\nFound",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = smartMatch?.reason ?: "Sustainable polymer acquisition",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reliabilityText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { reliabilityProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                text = "YOUR REQUIREMENT",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Eco-Processing Hub",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Material", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f))
                                Text("HDPE Grade A", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Volume", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f))
                                Text("12.5 Tons", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                text = "PERFECT MATCH",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "North-West Logistics",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Distance", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f))
                                Text("4.2 km", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Price Efficiency", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f))
                                Text("+14%", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
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
                                        color = MaterialTheme.colorScheme.onSurface,
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

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "MATCHING LOGIC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Proximity optimization reduces transport overhead and carbon footprint.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Partner quality and certification signals align with your current sourcing requirement.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = confidenceNote(insightState)
                                        ?: "Based on recent demand trend, this match has higher fulfillment probability.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Request Match", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("View Details", fontWeight = FontWeight.SemiBold)
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