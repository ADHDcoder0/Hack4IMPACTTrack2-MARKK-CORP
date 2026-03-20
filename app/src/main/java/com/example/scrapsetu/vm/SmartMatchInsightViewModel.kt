package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.SmartMatchInsight
import com.example.scrapsetu.data.repo.SmartMatchInsightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SmartMatchInsightState {
    object Idle : SmartMatchInsightState()
    object Loading : SmartMatchInsightState()
    data class Success(val insight: SmartMatchInsight) : SmartMatchInsightState()
    object Empty : SmartMatchInsightState()
    data class Error(val message: String) : SmartMatchInsightState()
}

@HiltViewModel
class SmartMatchInsightViewModel @Inject constructor(
    private val smartMatchInsightRepository: SmartMatchInsightRepository
) : ViewModel() {

    private val _insightState = MutableStateFlow<SmartMatchInsightState>(SmartMatchInsightState.Idle)
    val insightState: StateFlow<SmartMatchInsightState> = _insightState

    fun loadLatestInsight(listingId: String?) {
        if (listingId.isNullOrBlank()) {
            _insightState.value = SmartMatchInsightState.Empty
            return
        }

        viewModelScope.launch {
            _insightState.value = SmartMatchInsightState.Loading
            try {
                val insight = smartMatchInsightRepository.getLatestInsightForListing(listingId)
                _insightState.value = if (insight != null) {
                    SmartMatchInsightState.Success(insight)
                } else {
                    SmartMatchInsightState.Empty
                }
            } catch (e: Exception) {
                _insightState.value = SmartMatchInsightState.Error(
                    e.message ?: "Failed to load smart match insight"
                )
            }
        }
    }

    fun reset() {
        _insightState.value = SmartMatchInsightState.Idle
    }
}
