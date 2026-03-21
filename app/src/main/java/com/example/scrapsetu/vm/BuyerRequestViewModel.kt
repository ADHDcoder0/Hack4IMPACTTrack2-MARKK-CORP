package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.BuyerRequest
import com.example.scrapsetu.data.repo.AuthRepository
import com.example.scrapsetu.data.repo.BuyerRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BuyerRequestState {
    object Idle : BuyerRequestState()
    object Loading : BuyerRequestState()
    data class Success(val requests: List<BuyerRequest>) : BuyerRequestState()
    data class Error(val message: String) : BuyerRequestState()
}

@HiltViewModel
class BuyerRequestViewModel @Inject constructor(
    private val buyerRequestRepository: BuyerRequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BuyerRequestState>(BuyerRequestState.Idle)
    val state: StateFlow<BuyerRequestState> = _state

    private val _requests = MutableStateFlow<List<BuyerRequest>>(emptyList())
    val requests: StateFlow<List<BuyerRequest>> = _requests

    fun loadMyRequests(forceRefresh: Boolean = false) {
        if (_state.value is BuyerRequestState.Loading && !forceRefresh) return

        viewModelScope.launch {
            _state.value = BuyerRequestState.Loading
            try {
                val buyerId = authRepository.currentUserId() ?: throw Exception("Not logged in")
                val data = buyerRequestRepository.getMyRequests(buyerId)
                _requests.value = data
                _state.value = BuyerRequestState.Success(data)
            } catch (e: Exception) {
                _state.value = BuyerRequestState.Error(e.message ?: "Failed to load requests")
            }
        }
    }

    fun postRequest(
        wasteType: String,
        quantityKg: Double,
        maxPricePerKg: Double,
        location: String,
        description: String?
    ) {
        viewModelScope.launch {
            _state.value = BuyerRequestState.Loading
            try {
                val buyerId = authRepository.currentUserId() ?: throw Exception("Not logged in")
                buyerRequestRepository.createRequest(
                    BuyerRequest(
                        buyerId = buyerId,
                        wasteType = wasteType,
                        quantityKg = quantityKg,
                        maxPricePerKg = maxPricePerKg,
                        location = location.ifBlank { null },
                        description = description?.takeIf { it.isNotBlank() },
                        status = "open"
                    )
                )
                loadMyRequests(forceRefresh = true)
            } catch (e: Exception) {
                _state.value = BuyerRequestState.Error(e.message ?: "Failed to post request")
            }
        }
    }

    fun closeRequest(requestId: String) {
        viewModelScope.launch {
            _state.value = BuyerRequestState.Loading
            try {
                buyerRequestRepository.closeRequest(requestId)
                loadMyRequests(forceRefresh = true)
            } catch (e: Exception) {
                _state.value = BuyerRequestState.Error(e.message ?: "Failed to close request")
            }
        }
    }
}
