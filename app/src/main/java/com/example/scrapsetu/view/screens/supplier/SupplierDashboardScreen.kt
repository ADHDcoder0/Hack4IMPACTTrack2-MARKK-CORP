package com.example.scrapsetu.view.screens.supplier

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.vm.ListingState
import com.example.scrapsetu.vm.ListingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SupplierDashboardScreen(
    onSignOut: () -> Unit,
    onViewMatches: () -> Unit,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val listings by viewModel.listings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val isRefreshing = uiState is ListingState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.loadSupplierListings() }
    )

    LaunchedEffect(Unit) { viewModel.loadSupplierListings() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Listing",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (listings.isEmpty() && !isRefreshing) {
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
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDialog) {
        AddListingDialog(
            onDismiss = { showDialog = false },
            onSubmit = { wasteType, qty, price, location, desc, imageBytes, mimeType ->
                val capturedBytes = imageBytes?.copyOf()
                val capturedMime = mimeType ?: "image/jpeg"   // ← fix here

                Log.d("Dashboard", "Captured bytes: ${capturedBytes?.size}, mime: $capturedMime")

                showDialog = false

                viewModel.createListingWithImage(
                    wasteType, qty, price, location, desc,
                    capturedBytes, capturedMime
                )
            }
        )
    }
}

@Composable
fun ListingCard(listing: Listing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (listing.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = "Listing Image",
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
    onSubmit: (String, Double, Double, String, String, ByteArray?, String?) -> Unit
) {
    var wasteType by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var imageMimeType by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val context = LocalContext.current

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            imageMimeType = context.contentResolver.getType(it)
            context.contentResolver.openInputStream(it)?.use { stream ->
                imageBytes = stream.readBytes()
            }
            Log.d("Dialog", "Image picked — bytes: ${imageBytes?.size}, mime: $imageMimeType")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Listing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wasteType,
                    onValueChange = { wasteType = it },
                    label = { Text("Waste Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price per kg (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (imageBytes != null) {
                    AsyncImage(
                        model = selectedImageUri ?: imageBytes,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageBytes != null) "Change Image" else "Upload Image")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                Log.d("Dialog", "Submit — imageBytes: ${imageBytes?.size}, mime: $imageMimeType")
                onSubmit(
                    wasteType,
                    quantity.toDoubleOrNull() ?: 0.0,
                    price.toDoubleOrNull() ?: 0.0,
                    location,
                    description,
                    imageBytes,
                    imageMimeType
                )
            }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}