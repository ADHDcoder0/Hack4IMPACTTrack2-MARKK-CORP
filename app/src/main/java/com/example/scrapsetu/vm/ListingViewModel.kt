package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.repo.AuthRepository
import com.example.scrapsetu.data.repo.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListingViewModel @Inject constructor(
    private val listingRepo: ListingRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _uiState = MutableStateFlow<ListingState>(ListingState.Idle)
    val uiState: StateFlow<ListingState> = _uiState

    fun loadActiveListings() {
        viewModelScope.launch {
            _uiState.value = ListingState.Loading
            try {
                _listings.value = listingRepo.getActiveListings()
                _uiState.value = ListingState.Idle
            } catch (e: Exception) {
                _uiState.value = ListingState.Error(e.message ?: "Failed to load listings")
            }
        }
    }

    fun createListing(
        wasteType: String,
        quantityKg: Double,
        pricePerKg: Double,
        location: String,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.value = ListingState.Loading
            try {
                val supplierId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                listingRepo.createListing(
                    Listing(
                        supplierId = supplierId,
                        wasteType = wasteType,
                        quantityKg = quantityKg,
                        pricePerKg = pricePerKg,
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

    fun loadSupplierListings() {
        viewModelScope.launch {
            _uiState.value = ListingState.Loading
            try {
                val supplierId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                _listings.value = listingRepo.getSupplierListings(supplierId)
                _uiState.value = ListingState.Idle
            } catch (e: Exception) {
                _uiState.value = ListingState.Error(e.message ?: "Failed to load listings")
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