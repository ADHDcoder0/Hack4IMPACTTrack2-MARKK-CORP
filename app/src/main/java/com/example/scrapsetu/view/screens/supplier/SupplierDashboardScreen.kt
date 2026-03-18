package com.example.scrapsetu.view.screens.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.vm.ListingState
import com.example.scrapsetu.vm.ListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDashboardScreen(
    onSignOut: () -> Unit,
    viewModel: ListingViewModel = hiltViewModel(),
    onViewMatches: () -> Unit
) {
    val listings by viewModel.listings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadSupplierListings() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onViewMatches) {
                        Text("Requests", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    TextButton(onClick = onSignOut) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Listing", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState is ListingState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (listings.isEmpty()) {
                Text(
                    "No listings yet. Tap + to add one.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(listings) { listing ->
                        ListingCard(listing)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddListingDialog(
            onDismiss = { showDialog = false },
            onSubmit = { wasteType, qty, price, location, desc ->
                viewModel.createListing(wasteType, qty, price, location, desc)
                showDialog = false
            }
        )
    }
}

@Composable
fun ListingCard(listing: Listing) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(listing.wasteType, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${listing.quantityKg} kg @ ₹${listing.pricePerKg}/kg")
            Text("Location: ${listing.location}")
            Text("Status: ${listing.status}")
            if (listing.description.isNotEmpty()) {
                Text(listing.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AddListingDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Double, String, String) -> Unit
) {
    var wasteType by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Listing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = wasteType, onValueChange = { wasteType = it }, label = { Text("Waste Type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price per kg (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(
                    wasteType,
                    quantity.toDoubleOrNull() ?: 0.0,
                    price.toDoubleOrNull() ?: 0.0,
                    location,
                    description
                )
            }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}