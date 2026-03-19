package com.example.scrapsetu.view.screens.buyer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.vm.GroqState
import com.example.scrapsetu.vm.GroqViewModel
import com.example.scrapsetu.vm.ListingState
import com.example.scrapsetu.vm.ListingViewModel
import com.example.scrapsetu.vm.MatchState
import com.example.scrapsetu.vm.MatchViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BuyerDashboardScreen(
    onSignOut: () -> Unit,
    onViewMatches: () -> Unit,
    listingViewModel: ListingViewModel = hiltViewModel(),
    matchViewModel: MatchViewModel = hiltViewModel(),
    groqViewModel: GroqViewModel = hiltViewModel()
) {
    val groqState by groqViewModel.groqState.collectAsState()
    var selectedListing by remember { mutableStateOf<Listing?>(null) }
    val listings by listingViewModel.listings.collectAsState()
    val uiState by listingViewModel.uiState.collectAsState()
    val matchState by matchViewModel.matchState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val isRefreshing = uiState is ListingState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { listingViewModel.loadActiveListings() }
    )

    LaunchedEffect(Unit) { listingViewModel.loadActiveListings() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(matchState) {
        when (matchState) {
            is MatchState.Success -> snackbarHostState.showSnackbar("Match requested!")
            is MatchState.Error -> snackbarHostState.showSnackbar((matchState as MatchState.Error).message)
            else -> Unit
        }
    }

    if (selectedListing != null) {
        AlertDialog(
            onDismissRequest = {
                selectedListing = null
                groqViewModel.reset()
            },
            title = { Text("Smart Match Suggestion") },
            text = {
                when (groqState) {
                    is GroqState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                    is GroqState.Success -> {
                        Text((groqState as GroqState.Success).suggestion)
                    }
                    is GroqState.Error -> {
                        Text(
                            (groqState as GroqState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            },
            confirmButton = {
                Button(onClick = {
                    val id = selectedListing?.id ?: return@Button
                    matchViewModel.requestMatch(id)
                    selectedListing = null
                    groqViewModel.reset()
                }) { Text("Request Match") }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedListing = null
                    groqViewModel.reset()
                }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Listings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onViewMatches) {
                        Text("My Matches", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    TextButton(onClick = onSignOut) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search waste type...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            val filtered = listings.filter {
                it.wasteType.contains(searchQuery, ignoreCase = true) ||
                        it.location.contains(searchQuery, ignoreCase = true)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when {
                    uiState is ListingState.Loading && listings.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    filtered.isEmpty() -> {
                        Text("No listings found.", modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            items(filtered) { listing ->
                                BuyerListingCard(
                                    listing = listing,
                                    onRequestMatch = {
                                        val id = listing.id ?: return@BuyerListingCard
                                        matchViewModel.requestMatch(id)
                                    },
                                    onSmartMatch = {
                                        selectedListing = listing
                                        groqViewModel.getSuggestion(listing)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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
}

@Composable
fun BuyerListingCard(
    listing: Listing,
    onRequestMatch: () -> Unit,
    onSmartMatch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (listing.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = "Waste image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(listing.wasteType, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${listing.quantityKg} kg @ ₹${listing.pricePerKg}/kg")
            Text("Location: ${listing.location}")
            if (listing.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(listing.description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRequestMatch,
                    modifier = Modifier.weight(1f)
                ) { Text("Request Match") }
                OutlinedButton(
                    onClick = onSmartMatch,
                    modifier = Modifier.weight(1f)
                ) { Text("Smart Match") }
            }
        }
    }
}