package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.repo.AuthRepository
import com.example.scrapsetu.data.repo.ListingRepository
import com.example.scrapsetu.data.repo.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListingViewModel @Inject constructor(
    private val listingRepo: ListingRepository,
    private val authRepo: AuthRepository,
    private val storageRepo: StorageRepository

) : ViewModel() {

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _uiState = MutableStateFlow<ListingState>(ListingState.Idle)
    val uiState: StateFlow<ListingState> = _uiState

    private var isLoadingActiveListings = false
    private var isLoadingSupplierListings = false

    fun loadActiveListings(forceRefresh: Boolean = false) {
        if (isLoadingActiveListings && !forceRefresh) return
        viewModelScope.launch {
            isLoadingActiveListings = true
            _uiState.value = ListingState.Loading
            try {
                _listings.value = listingRepo.getActiveListings()
                _uiState.value = ListingState.Idle
            } catch (e: Exception) {
                _uiState.value = ListingState.Error(e.message ?: "Failed to load listings")
            } finally {
                isLoadingActiveListings = false
            }
        }
    }

    fun createListing(
        wasteType: String,
        quantityKg: Double,
        pricePerKg: Double,
        location: String,
        description: String,
        wasteCategoryId: String? = null,
        stateCode: String? = null,
        districtId: String? = null,
        townCity: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = ListingState.Loading
            try {
                val supplierId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                listingRepo.createListing(
                    Listing(
                        supplierId = supplierId,
                        wasteType = wasteType,
                        wasteCategoryId = wasteCategoryId,
                        quantityKg = quantityKg,
                        pricePerKg = pricePerKg,
                        stateCode = stateCode,
                        districtId = districtId,
                        townCity = townCity,
                        location = location,
                        description = description
                    )
                )
                _uiState.value = ListingState.Success
            } catch (e: Exception) {
                _uiState.value = ListingState.Error(e.message ?: "Failed to create listing")
            }
        }
    }

    fun loadSupplierListings(forceRefresh: Boolean = false) {
        if (isLoadingSupplierListings && !forceRefresh) return
        viewModelScope.launch {
            isLoadingSupplierListings = true
            _uiState.value = ListingState.Loading
            try {
                val supplierId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                _listings.value = listingRepo.getSupplierListings(supplierId)
                _uiState.value = ListingState.Idle
            } catch (e: Exception) {
                _uiState.value = ListingState.Error(e.message ?: "Failed to load listings")
            } finally {
                isLoadingSupplierListings = false
            }
        }
    }

    fun createListingWithImage(
        wasteType: String,
        quantityKg: Double,
        pricePerKg: Double,
        location: String,
        description: String,
        imageBytes: ByteArray?,
        mimeType: String = "image/jpeg",
        wasteCategoryId: String? = null,
        stateCode: String? = null,
        districtId: String? = null,
        townCity: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = ListingState.Loading
            try {
                android.util.Log.d("ListingVM", "imageBytes null: ${imageBytes == null}")
                val supplierId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                android.util.Log.d("ListingVM", "supplierId: $supplierId")
                val imageUrl = if (imageBytes != null) {
                    storageRepo.uploadImage(imageBytes, mimeType, supplierId)
                } else ""
                android.util.Log.d("ListingVM", "imageUrl: $imageUrl")
                listingRepo.createListing(
                    Listing(
                        supplierId = supplierId,
                        wasteType = wasteType,
                        wasteCategoryId = wasteCategoryId,
                        quantityKg = quantityKg,
                        pricePerKg = pricePerKg,
                        stateCode = stateCode,
                        districtId = districtId,
                        townCity = townCity,
                        location = location,
                        description = description,
                        imageUrl = imageUrl
                    )
                )
                android.util.Log.d("ListingVM", "Listing created successfully")
                // Fetch fresh list inline — no separate coroutine, no race condition
                _listings.value = listingRepo.getSupplierListings(supplierId)
                _uiState.value = ListingState.Idle
            } catch (e: Exception) {
                android.util.Log.e("ListingVM", "Error: ${e.message}")
                _uiState.value = ListingState.Error(e.message ?: "Failed to create listing")
            }
        }
    }

    fun updateListingWithImage(
        listingId: String,
        wasteType: String,
        quantityKg: Double,
        pricePerKg: Double,
        location: String,
        description: String,
        imageBytes: ByteArray?,
        existingImageUrl: String,
        mimeType: String = "image/jpeg",
        wasteCategoryId: String? = null,
        stateCode: String? = null,
        districtId: String? = null,
        townCity: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = ListingState.Loading
            try {
                val supplierId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                val imageUrl = if (imageBytes != null) {
                    storageRepo.uploadImage(imageBytes, mimeType, supplierId)
                } else {
                    existingImageUrl
                }

                listingRepo.updateListingDetails(
                    listingId = listingId,
                    wasteType = wasteType,
                    wasteCategoryId = wasteCategoryId,
                    quantityKg = quantityKg,
                    pricePerKg = pricePerKg,
                    stateCode = stateCode,
                    districtId = districtId,
                    townCity = townCity,
                    location = location,
                    description = description,
                    imageUrl = imageUrl
                )

                _listings.value = listingRepo.getSupplierListings(supplierId)
                _uiState.value = ListingState.Idle
            } catch (e: Exception) {
                _uiState.value = ListingState.Error(e.message ?: "Failed to update listing")
            }
        }
    }
}

sealed class ListingState {
    object Idle : ListingState()
    object Loading : ListingState()
    object Success : ListingState()
    data class Error(val message: String) : ListingState()
}