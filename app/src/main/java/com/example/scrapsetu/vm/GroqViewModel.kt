package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.PriceEstimate
import com.example.scrapsetu.data.repo.GroqRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GroqState {
    object Idle : GroqState()
    object Loading : GroqState()
    data class Success(val suggestion: String) : GroqState()
    data class Error(val message: String) : GroqState()
}

@HiltViewModel
class GroqViewModel @Inject constructor(
    private val groqRepo: GroqRepository
) : ViewModel() {

    private val _groqState = MutableStateFlow<GroqState>(GroqState.Idle)
    val groqState: StateFlow<GroqState> = _groqState

    private val _priceEstimate = MutableStateFlow<PriceEstimate?>(null)
    val priceEstimate: StateFlow<PriceEstimate?> = _priceEstimate

    private val _priceEstimateLoading = MutableStateFlow(false)
    val priceEstimateLoading: StateFlow<Boolean> = _priceEstimateLoading

    private val _priceEstimateError = MutableStateFlow<String?>(null)
    val priceEstimateError: StateFlow<String?> = _priceEstimateError

    fun getSuggestion(listing: Listing) {
        viewModelScope.launch {
            _groqState.value = GroqState.Loading
            try {
                val suggestion = groqRepo.getSmartMatchSuggestion(listing)
                _groqState.value = GroqState.Success(suggestion)
            } catch (e: Exception) {
                _groqState.value = GroqState.Error(e.message ?: "Failed to get suggestion")
            }
        }
    }

    fun reset() {
        _groqState.value = GroqState.Idle
        _priceEstimate.value = null
        _priceEstimateLoading.value = false
        _priceEstimateError.value = null
    }

    fun requestPriceEstimate(materialType: String, quantity: Double, state: String) {
        if (materialType.isBlank() || quantity <= 0.0 || state.isBlank()) {
            _priceEstimate.value = null
            _priceEstimateError.value = null
            _priceEstimateLoading.value = false
            return
        }

        viewModelScope.launch {
            _priceEstimateLoading.value = true
            _priceEstimateError.value = null
            try {
                _priceEstimate.value = groqRepo.getPriceEstimate(materialType, quantity, state)
            } catch (e: Exception) {
                _priceEstimate.value = null
                _priceEstimateError.value = e.message ?: "Failed to estimate price"
            } finally {
                _priceEstimateLoading.value = false
            }
        }
    }
}