package com.example.scrapsetu.view.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.Match
import com.example.scrapsetu.vm.MatchState
import com.example.scrapsetu.vm.MatchViewModel
import com.example.scrapsetu.vm.ListingViewModel
import com.example.scrapsetu.vm.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchStatusScreen(
    onBack: () -> Unit,
    onOpenChat: (matchId: String, otherUserId: String, otherUserName: String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: MatchViewModel = hiltViewModel(),
    listingViewModel: ListingViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
) {
    val matches by viewModel.matches.collectAsState()
    val matchState by viewModel.matchState.collectAsState()
    val listings by listingViewModel.listings.collectAsState()
    val usersById by userViewModel.usersById.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listingById = remember(listings) {
        listings.mapNotNull { listing ->
            listing.id?.let { id -> id to listing }
        }.toMap()
    }
    val supplierIds = remember(matches, listingById) {
        matches.mapNotNull { match ->
            listingById[match.listingId]?.supplierId?.takeIf { it.isNotBlank() }
        }.distinct().sorted()
    }

    val onActionClick: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(Unit) { viewModel.loadMyMatches() }
    LaunchedEffect(Unit) { listingViewModel.loadActiveListings() }
    LaunchedEffect(supplierIds) { userViewModel.loadUsersByIds(supplierIds) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "My Matches",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                }
            }
        },
        bottomBar = {
            MatchBottomBar(
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToMatches = {},
                onNavigateToProfile = onOpenProfile
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (matchState is MatchState.Loading && matches.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OverviewCards(
                            onViewImpactReport = { onActionClick("Impact report export is not ready yet") }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Match Status",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { onActionClick("Advanced filters are coming soon") }) {
                                Text("Filter")
                                Icon(Icons.Filled.FilterList, contentDescription = null)
                            }
                        }
                    }

                    if (matches.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No matches yet.",
                                    modifier = Modifier.padding(18.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        itemsIndexed(matches) { index, match ->
                            MatchCard(
                                match = match,
                                index = index,
                                onArchive = { onActionClick("Archived") },
                                onRevertRequest = {
                                    viewModel.revertMyRequest(match.id)
                                    onActionClick("Request reverted")
                                },
                                onPrimaryAction = {
                                    onActionClick(
                                        if (match.status == "confirmed") "Invoice workflow is not connected yet" else "Detailed match view is not connected yet"
                                    )
                                },
                                onChat = {
                                    val supplierId = listingById[match.listingId]?.supplierId.orEmpty()
                                    if (supplierId.isBlank()) {
                                        onActionClick("Chat is unavailable for this match")
                                    } else {
                                        val supplierName = usersById[supplierId]?.name?.ifBlank { "Supplier" } ?: "Supplier"
                                        onOpenChat(match.id, supplierId, supplierName)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        SmartMatchFoundBanner(
                            onReview = { onActionClick("Smart match review panel is not available yet") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCards(onViewImpactReport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TOTAL SAVINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹14,500",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("12% from last month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sustainable Network",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You've diverted 1.2 tons of industrial plastic from landfills this quarter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onViewImpactReport,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("View Impact Report")
                }
            }
        }
    }
}

@Composable
private fun MatchCard(
    match: Match,
    index: Int,
    onArchive: () -> Unit,
    onRevertRequest: () -> Unit,
    onPrimaryAction: () -> Unit,
    onChat: () -> Unit
) {
    val statusContainerColor = when (match.status) {
        "confirmed" -> MaterialTheme.colorScheme.secondary
        "rejected" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
    }
    val statusTextColor = when (match.status) {
        "confirmed" -> MaterialTheme.colorScheme.onSecondary
        "rejected" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onBackground
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                            Text(
                                text = "${match.status.replaceFirstChar { it.uppercase() }} request",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (index % 3) {
                            0 -> "Premium Grade PET Pellets"
                            1 -> "Industrial Copper Scraps"
                            else -> "Mixed Cardboard Waste"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (match.status == "rejected") {
                        Text(
                            text = "Reason: Quality check failed for contamination levels.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (index % 2 == 0) "500kg" else "1.2 Tons", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (match.status == "confirmed") Icons.Filled.Event else Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (match.status == "confirmed") "Jun 24, 2024" else "Okhla Ind. Area", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Surface(color = statusContainerColor, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        match.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (match.status == "rejected") {
                Button(
                    onClick = onArchive,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Archive")
                }
            } else if (match.status == "pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRevertRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Revert Request")
                    }
                    OutlinedButton(
                        onClick = onChat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(if (match.status == "confirmed") "Invoice" else "Details")
                    }
                    OutlinedButton(
                        onClick = onChat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat")
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartMatchFoundBanner(onReview: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(21.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Match Found!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "A buyer in Noida matches 95% of your HDPE inventory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onReview,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Review")
                }
            }
        }
    }
}

@Composable
private fun MatchBottomBar(
    onNavigateToDashboard: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToProfile: () -> Unit
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
            MatchNavItem(icon = Icons.Filled.Dashboard, label = "Dashboard", selected = false, onClick = onNavigateToDashboard)
            MatchNavItem(icon = Icons.Filled.Handshake, label = "Matches", selected = true, onClick = onNavigateToMatches)
            MatchNavItem(icon = Icons.Filled.Person, label = "Profile", selected = false, onClick = onNavigateToProfile)
        }
    }
}

@Composable
private fun MatchNavItem(
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