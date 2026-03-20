package com.example.scrapsetu.view.screens.supplier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.Match
import com.example.scrapsetu.data.model.User
import com.example.scrapsetu.view.components.AnalyticsShell
import com.example.scrapsetu.view.components.BuyerSuggestionsPanel
import com.example.scrapsetu.vm.AuthViewModel
import com.example.scrapsetu.vm.GroqAnalyticsViewModel
import com.example.scrapsetu.vm.ListingViewModel
import com.example.scrapsetu.vm.MatchState
import com.example.scrapsetu.vm.MatchViewModel
import com.example.scrapsetu.vm.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierMatchScreen(
    onBack: () -> Unit,
    onOpenChat: (matchId: String, otherUserId: String, otherUserName: String) -> Unit,
    onNavigateToMarketplace: () -> Unit,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit,
    listingViewModel: ListingViewModel = hiltViewModel(),
    matchViewModel: MatchViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    analyticsViewModel: GroqAnalyticsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val listings by listingViewModel.listings.collectAsState()
    val supplierMatches by matchViewModel.supplierMatches.collectAsState()
    val matchState by matchViewModel.matchState.collectAsState()
    val usersById by userViewModel.usersById.collectAsState()
    val analyticsState by analyticsViewModel.state.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val listingIds = remember(listings) { listings.mapNotNull { it.id }.distinct().sorted() }
    val buyerIds = remember(supplierMatches) { supplierMatches.map { it.buyerId }.distinct().sorted() }

    val listingById = remember(listings) {
        listings.mapNotNull { listing ->
            listing.id?.let { id -> id to listing }
        }.toMap()
    }

    val pendingCount = supplierMatches.count { it.status == "pending" }
    val activeCount = supplierMatches.count { it.status != "rejected" }
    val confirmedCount = supplierMatches.count { it.status == "confirmed" }
    val conversionRate = if (supplierMatches.isNotEmpty()) {
        (confirmedCount * 100) / supplierMatches.size
    } else {
        0
    }

    LaunchedEffect(Unit) {
        listingViewModel.loadSupplierListings()
    }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUserDetails()
    }

    LaunchedEffect(listingIds) {
        matchViewModel.loadSupplierMatches(listingIds)
    }

    LaunchedEffect(buyerIds) {
        userViewModel.loadUsersByIds(buyerIds)
    }

    LaunchedEffect(currentUser?.id, listingIds) {
        currentUser?.id?.let { userId ->
            analyticsViewModel.load(
                userId = userId,
                listingId = listingIds.firstOrNull()
            )
        }
    }

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
                            text = "Match Requests",
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
            SupplierMatchesBottomBar(
                onNavigateToMarketplace = onNavigateToMarketplace,
                onNavigateToMatches = {},
                onNavigateToProfile = onOpenProfile
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (matchState is MatchState.Loading && supplierMatches.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SummaryStatsRow(
                            activeCount = activeCount,
                            pendingCount = pendingCount,
                            conversionRate = conversionRate
                        )
                    }

                    item {
                        AnalyticsShell(state = analyticsState) { data ->
                            BuyerSuggestionsPanel(suggestions = data.buyerSuggestions)
                        }
                    }

                    if (supplierMatches.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No match requests yet.",
                                    modifier = Modifier.padding(18.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        itemsIndexed(supplierMatches) { _, match ->
                            SupplierMatchCard(
                                match = match,
                                listing = listingById[match.listingId],
                                buyer = usersById[match.buyerId],
                                onConfirm = { matchViewModel.updateMatch(match.id, "confirmed") },
                                onReject = { matchViewModel.updateMatch(match.id, "rejected") },
                                onChat = {
                                    val otherUserName = usersById[match.buyerId]?.name?.ifBlank { "Buyer" } ?: "Buyer"
                                    onOpenChat(match.id, match.buyerId, otherUserName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatsRow(
    activeCount: Int,
    pendingCount: Int,
    conversionRate: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            label = "ACTIVE MATCHES",
            value = activeCount.toString(),
            valueColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Filled.Handshake, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f), modifier = Modifier.size(52.dp)) }
        )
        StatTile(
            label = "PENDING APPROVAL",
            value = pendingCount.toString().padStart(2, '0'),
            valueColor = MaterialTheme.colorScheme.tertiary,
            icon = { Icon(Icons.Filled.PendingActions, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f), modifier = Modifier.size(52.dp)) }
        )
        StatTile(
            label = "CONVERSION RATE",
            value = "$conversionRate%",
            valueColor = MaterialTheme.colorScheme.secondary,
            icon = { Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f), modifier = Modifier.size(52.dp)) }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    icon: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor, fontWeight = FontWeight.ExtraBold)
            }
            icon()
        }
    }
}

@Composable
private fun SupplierMatchCard(
    match: Match,
    listing: Listing?,
    buyer: User?,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column {
            Box {
                AsyncImage(
                    model = listing?.imageUrl?.takeIf { it.isNotBlank() },
                    contentDescription = "Listing photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.08f))
                )

                Surface(
                    color = statusContainerColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = match.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = statusTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("LISTING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                        Text(
                            listing?.wasteType ?: "Requested material",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BUYER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                        Text(
                            buyer?.name?.ifBlank { "Buyer" } ?: "Buyer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (listing != null) "${listing.quantityKg} kg ${listing.wasteType}" else "Requested listing material",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buyer?.location?.ifBlank { listing?.location ?: "Location unavailable" }
                            ?: listing?.location
                            ?: "Location unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (match.status.equals("pending", ignoreCase = true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = onChat,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat with Buyer", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (match.status.equals("confirmed", ignoreCase = true)) {
                                "Request has been confirmed"
                            } else {
                                "Request is no longer pending"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        )
                    }
                    OutlinedButton(
                        onClick = onChat,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat with Buyer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierMatchesBottomBar(
    onNavigateToMarketplace: () -> Unit,
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
            SupplierMatchNavItem(icon = Icons.Filled.Storefront, selected = false, onClick = onNavigateToMarketplace)
            SupplierMatchNavItem(icon = Icons.Filled.Handshake, selected = true, onClick = onNavigateToMatches)
            SupplierMatchNavItem(icon = Icons.Filled.Person, selected = false, onClick = onNavigateToProfile)
        }
    }
}

@Composable
private fun SupplierMatchNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = null)
    }
}