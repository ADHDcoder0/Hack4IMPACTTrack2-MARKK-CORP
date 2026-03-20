package com.example.scrapsetu.view.screens.supplier

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.scrapsetu.data.model.IndiaDistrict
import com.example.scrapsetu.data.model.IndiaState
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.WasteCategory
import com.example.scrapsetu.view.components.AnalyticsShell
import com.example.scrapsetu.view.components.ImageDetectionPicker
import com.example.scrapsetu.view.components.PriceSuggestionChip
import com.example.scrapsetu.view.components.SellerAnalyticsCard
import com.example.scrapsetu.vm.AnalyticsUiState
import com.example.scrapsetu.vm.AuthViewModel
import com.example.scrapsetu.vm.GroqAnalyticsViewModel
import com.example.scrapsetu.vm.ImageDetectionViewModel
import com.example.scrapsetu.vm.ListingState
import com.example.scrapsetu.vm.ListingViewModel
import com.example.scrapsetu.vm.MasterDataViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SupplierDashboardScreen(
    onSignOut: () -> Unit,
    onViewMatches: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: ListingViewModel = hiltViewModel(),
    masterDataViewModel: MasterDataViewModel = hiltViewModel(),
    analyticsViewModel: GroqAnalyticsViewModel = hiltViewModel(),
    detectionViewModel: ImageDetectionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val listings by viewModel.listings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val categories by masterDataViewModel.categories.collectAsState()
    val states by masterDataViewModel.states.collectAsState()
    val districts by masterDataViewModel.districts.collectAsState()
    val analyticsState by analyticsViewModel.state.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingListing by remember { mutableStateOf<Listing?>(null) }

    val isRefreshing = uiState is ListingState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.loadSupplierListings(forceRefresh = true) }
    )

    LaunchedEffect(Unit) { viewModel.loadSupplierListings() }
    LaunchedEffect(Unit) { masterDataViewModel.loadInitialData() }
    LaunchedEffect(Unit) { authViewModel.loadCurrentUserDetails() }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            analyticsViewModel.load(userId = userId)
        }
    }

    val filteredListings = listings.filter {
        it.wasteType.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true) ||
                (it.townCity ?: "").contains(searchQuery, ignoreCase = true)
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
                        Text(
                            text = "ScrapSetu",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text("Add New Listing", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Listing") }
            )
        },
        bottomBar = {
            SupplierBottomBar(
                onViewMatches = onViewMatches,
                onOpenMarketplace = onOpenMarketplace,
                onOpenProfile = onOpenProfile
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (isRefreshing && listings.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "My Listings",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manage your industrial waste inventory and track active bids.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search materials...") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        AnalyticsShell(state = analyticsState) { data ->
                            SellerAnalyticsCard(analytics = data.sellerAnalytics)
                        }
                    }

                    if (filteredListings.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No listings yet. Tap Add New Listing to create one.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        items(filteredListings) { listing ->
                            ListingCard(
                                listing = listing,
                                onPrimaryAction = onViewMatches,
                                onEdit = {
                                    editingListing = listing
                                    showDialog = true
                                }
                            )
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

    if (showDialog) {
        AddListingDialog(
            categories = categories,
            states = states,
            districts = districts,
            analyticsState = analyticsState,
            detectionViewModel = detectionViewModel,
            initialListing = editingListing,
            onStateSelected = { stateCode -> masterDataViewModel.loadDistricts(stateCode) },
            onDismiss = {
                showDialog = false
                editingListing = null
                detectionViewModel.reset()
            },
            onSubmit = { wasteType, wasteCategoryId, qty, price, location, desc, stateCode, districtId, townCity, imageBytes, mimeType ->
                val capturedBytes = imageBytes?.copyOf()
                val capturedMime = mimeType ?: "image/jpeg"   // ← fix here

                Log.d("Dashboard", "Captured bytes: ${capturedBytes?.size}, mime: $capturedMime")

                showDialog = false
                detectionViewModel.reset()
                val listingToEdit = editingListing
                editingListing = null

                if (listingToEdit == null) {
                    viewModel.createListingWithImage(
                        wasteType = wasteType,
                        quantityKg = qty,
                        pricePerKg = price,
                        location = location,
                        description = desc,
                        imageBytes = capturedBytes,
                        mimeType = capturedMime,
                        wasteCategoryId = wasteCategoryId,
                        stateCode = stateCode,
                        districtId = districtId,
                        townCity = townCity
                    )
                } else {
                    val listingId = listingToEdit.id ?: return@AddListingDialog
                    viewModel.updateListingWithImage(
                        listingId = listingId,
                        wasteType = wasteType,
                        quantityKg = qty,
                        pricePerKg = price,
                        location = location,
                        description = desc,
                        imageBytes = capturedBytes,
                        existingImageUrl = listingToEdit.imageUrl,
                        mimeType = capturedMime,
                        wasteCategoryId = wasteCategoryId,
                        stateCode = stateCode,
                        districtId = districtId,
                        townCity = townCity
                    )
                }
            }
        )
    }
}

@Composable
private fun ListingCard(
    listing: Listing,
    onPrimaryAction: () -> Unit,
    onEdit: () -> Unit
) {
    val statusInfo = listingStatusInfo(listing.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box {
                if (listing.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = "Listing Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusInfo.badgeColor,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(
                        text = statusInfo.badgeText,
                        color = statusInfo.badgeTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.wasteType,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${listing.pricePerKg}/kg",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listing.description.ifBlank { "Industrial material ready for pickup." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${listing.quantityKg} kg", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(listing.location, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(14.dp))

                val buttonColors = if (statusInfo.primaryActionEnabled) {
                    ButtonDefaults.buttonColors(containerColor = statusInfo.primaryActionColor)
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = statusInfo.primaryActionColor,
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = onPrimaryAction,
                    enabled = statusInfo.primaryActionEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = buttonColors
                ) {
                    Text(statusInfo.primaryActionText, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("Edit Details", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class ListingStatusInfo(
    val badgeText: String,
    val badgeColor: Color,
    val badgeTextColor: Color,
    val primaryActionText: String,
    val primaryActionColor: Color,
    val primaryActionEnabled: Boolean
)

@Composable
private fun listingStatusInfo(status: String): ListingStatusInfo {
    return when (status.lowercase()) {
        "active" -> ListingStatusInfo(
            badgeText = "Active",
            badgeColor = MaterialTheme.colorScheme.secondary,
            badgeTextColor = MaterialTheme.colorScheme.onSecondary,
            primaryActionText = "View Match Requests",
            primaryActionColor = MaterialTheme.colorScheme.primary,
            primaryActionEnabled = true
        )

        "matched" -> ListingStatusInfo(
            badgeText = "Pending Review",
            badgeColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
            badgeTextColor = MaterialTheme.colorScheme.tertiary,
            primaryActionText = "Awaiting Verification",
            primaryActionColor = MaterialTheme.colorScheme.surfaceVariant,
            primaryActionEnabled = false
        )

        else -> ListingStatusInfo(
            badgeText = "Expired",
            badgeColor = MaterialTheme.colorScheme.error,
            badgeTextColor = MaterialTheme.colorScheme.onError,
            primaryActionText = "Relist Now",
            primaryActionColor = MaterialTheme.colorScheme.primary,
            primaryActionEnabled = true
        )
    }
}

@Composable
private fun SupplierBottomBar(
    onViewMatches: () -> Unit,
    onOpenMarketplace: () -> Unit,
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
            SupplierNavItem(icon = Icons.Filled.Storefront, label = "Marketplace", selected = true, onClick = onOpenMarketplace)
            SupplierNavItem(icon = Icons.Filled.Handshake, label = "Matches", selected = false, onClick = onViewMatches)
            SupplierNavItem(icon = Icons.Filled.Person, label = "Profile", selected = false, onClick = onOpenProfile)
        }
    }
}

@Composable
private fun SupplierNavItem(
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

@Composable
fun AddListingDialog(
    categories: List<WasteCategory>,
    states: List<IndiaState>,
    districts: List<IndiaDistrict>,
    analyticsState: AnalyticsUiState,
    detectionViewModel: ImageDetectionViewModel,
    initialListing: Listing? = null,
    onStateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String?, Double, Double, String, String, String?, String?, String?, ByteArray?, String?) -> Unit
) {
    var selectedCategory by remember(initialListing, categories) {
        mutableStateOf(categories.firstOrNull { it.id == initialListing?.wasteCategoryId })
    }
    var selectedState by remember(initialListing, states) {
        mutableStateOf(states.firstOrNull { it.stateCode == initialListing?.stateCode })
    }
    var selectedDistrict by remember(initialListing, districts) {
        mutableStateOf(districts.firstOrNull { it.id == initialListing?.districtId })
    }
    var quantity by remember(initialListing) { mutableStateOf(initialListing?.quantityKg?.toString().orEmpty()) }
    var price by remember(initialListing) { mutableStateOf(initialListing?.pricePerKg?.toString().orEmpty()) }
    var townCity by remember(initialListing) { mutableStateOf(initialListing?.townCity.orEmpty()) }
    var description by remember(initialListing) { mutableStateOf(initialListing?.description.orEmpty()) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var imageMimeType by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val existingImageUrl = initialListing?.imageUrl.orEmpty()
    val isEditMode = initialListing != null

    val context = LocalContext.current

    LaunchedEffect(initialListing?.stateCode) {
        initialListing?.stateCode?.takeIf { it.isNotBlank() }?.let { onStateSelected(it) }
    }

    LaunchedEffect(initialListing?.districtId, districts) {
        if (selectedDistrict == null && initialListing?.districtId != null) {
            selectedDistrict = districts.firstOrNull { it.id == initialListing.districtId }
        }
    }

    LaunchedEffect(initialListing?.wasteCategoryId, categories) {
        if (selectedCategory == null && initialListing?.wasteCategoryId != null) {
            selectedCategory = categories.firstOrNull { it.id == initialListing.wasteCategoryId }
        }
    }

    LaunchedEffect(initialListing?.stateCode, states) {
        if (selectedState == null && initialListing?.stateCode != null) {
            selectedState = states.firstOrNull { it.stateCode == initialListing.stateCode }
        }
    }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = if (isEditMode) "Edit Listing" else "New Listing",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Add clear details for faster matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        if (!isEditMode) {
                            ImageDetectionPicker(
                                detectionVm = detectionViewModel,
                                onDetected = { slug, label, detectedDescription, minPrice, maxPrice, _ ->
                                    val category = categories.firstOrNull {
                                        it.slug.equals(slug, ignoreCase = true)
                                    } ?: categories.firstOrNull {
                                        it.label.equals(label, ignoreCase = true)
                                    }
                                    if (category != null) {
                                        selectedCategory = category
                                    }
                                    description = detectedDescription
                                    price = ((minPrice + maxPrice) / 2.0).toString()
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            if (imageBytes != null || existingImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = selectedImageUri ?: imageBytes ?: existingImageUrl,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(30.dp),
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f),
                                        modifier = Modifier.size(88.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Upload Waste Photo", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("PNG, JPG up to 10MB", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                if (imageBytes != null || existingImageUrl.isNotBlank()) {
                                    "Change Photo"
                                } else {
                                    "Upload Photo"
                                }
                            )
                        }
                    }

                    item {
                        Text("Waste Type", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        StructuredDropdownField(
                            label = "Material",
                            options = categories,
                            selectedOptionLabel = selectedCategory?.label,
                            optionLabel = { it.label },
                            onOptionSelected = { selectedCategory = it }
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                label = { Text("Quantity (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Price (₹/kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        AnalyticsShell(state = analyticsState) { data ->
                            PriceSuggestionChip(suggestion = data.priceSuggestion)
                        }
                    }

                    item {
                        Text("Location", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        StructuredDropdownField(
                            label = "State",
                            options = states,
                            selectedOptionLabel = selectedState?.stateName,
                            optionLabel = { it.stateName },
                            onOptionSelected = {
                                selectedState = it
                                selectedDistrict = null
                                onStateSelected(it.stateCode)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        StructuredDropdownField(
                            label = "District",
                            options = districts,
                            selectedOptionLabel = selectedDistrict?.districtName,
                            optionLabel = { it.districtName },
                            enabled = selectedState != null,
                            onOptionSelected = { selectedDistrict = it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = townCity,
                            onValueChange = { townCity = it },
                            placeholder = { Text("Enter Town / City") },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("Description", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Mention material condition, contamination levels, or loading assistance...") },
                            shape = RoundedCornerShape(22.dp),
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Smart Match Insight", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Based on current market trends in Mumbai, PET listings with clear photos sell 24% faster.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                                )
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(44.dp))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                Log.d("Dialog", "Submit — imageBytes: ${imageBytes?.size}, mime: $imageMimeType")
                                val wasteType = selectedCategory?.label
                                    ?: initialListing?.wasteType
                                    ?: ""
                                val computedLocation = listOfNotNull(
                                    townCity.takeIf { it.isNotBlank() },
                                    selectedDistrict?.districtName,
                                    selectedState?.stateName
                                ).joinToString(", ")
                                val resolvedLocation = computedLocation.ifBlank {
                                    initialListing?.location.orEmpty()
                                }
                                onSubmit(
                                    wasteType,
                                    selectedCategory?.id ?: initialListing?.wasteCategoryId,
                                    quantity.toDoubleOrNull() ?: 0.0,
                                    price.toDoubleOrNull() ?: 0.0,
                                    resolvedLocation,
                                    description,
                                    selectedState?.stateCode ?: initialListing?.stateCode,
                                    selectedDistrict?.id ?: initialListing?.districtId,
                                    townCity.ifBlank { null },
                                    imageBytes,
                                    imageMimeType
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                if (isEditMode) "Save Changes" else "Create Listing",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> StructuredDropdownField(
    label: String,
    options: List<T>,
    selectedOptionLabel: String?,
    optionLabel: (T) -> String,
    enabled: Boolean = true,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOptionLabel.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text("Select $label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}