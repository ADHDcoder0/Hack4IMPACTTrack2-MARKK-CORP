package com.example.scrapsetu.view.screens.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.scrapsetu.data.model.Match
import com.example.scrapsetu.vm.ListingViewModel
import com.example.scrapsetu.vm.MatchState
import com.example.scrapsetu.vm.MatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierMatchScreen(
    onBack: () -> Unit,
    listingViewModel: ListingViewModel = hiltViewModel(),
    matchViewModel: MatchViewModel = hiltViewModel()
) {
    val listings by listingViewModel.listings.collectAsState()
    val supplierMatches by matchViewModel.supplierMatches.collectAsState()
    val matchState by matchViewModel.matchState.collectAsState()

    LaunchedEffect(Unit) {
        listingViewModel.loadSupplierListings()
    }

    LaunchedEffect(listings) {
        if (listings.isNotEmpty()) {
            matchViewModel.loadSupplierMatches(listings.mapNotNull { it.id })        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Requests") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                matchState is MatchState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                supplierMatches.isEmpty() -> {
                    Text(
                        "No match requests yet.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(supplierMatches) { match ->
                            SupplierMatchCard(
                                match = match,
                                onConfirm = { matchViewModel.updateMatch(match.id, "confirmed") },
                                onReject = { matchViewModel.updateMatch(match.id, "rejected") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierMatchCard(
    match: Match,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = when (match.status) {
        "confirmed" -> MaterialTheme.colorScheme.secondary
        "rejected" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Buyer ID: ${match.buyerId.take(8)}...", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Listing: ${match.listingId.take(8)}...")
            Spacer(modifier = Modifier.height(8.dp))
            Badge(containerColor = statusColor) {
                Text(
                    match.status.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (match.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Confirm") }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f)
                    ) { Text("Reject") }
                }
            }
        }
    }
}