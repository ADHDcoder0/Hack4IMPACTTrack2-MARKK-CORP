package com.example.scrapsetu.view.screens.buyer

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
import com.example.scrapsetu.vm.MatchState
import com.example.scrapsetu.vm.MatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchStatusScreen(
    onBack: () -> Unit,
    viewModel: MatchViewModel = hiltViewModel()
) {
    val matches by viewModel.matches.collectAsState()
    val matchState by viewModel.matchState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMyMatches() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Matches") },
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
                matches.isEmpty() -> {
                    Text(
                        "No matches yet.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(matches) { match ->
                            MatchCard(match)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(match: Match) {
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
            Text("Match ID: ${match.id.take(8)}...", style = MaterialTheme.typography.titleSmall)
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
        }
    }
}