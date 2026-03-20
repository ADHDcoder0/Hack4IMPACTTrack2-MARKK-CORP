package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.AnalyticsResponse
import com.example.scrapsetu.data.repo.GroqAnalyticsRepository.AnalyticsSource
import com.example.scrapsetu.data.repo.GroqAnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AnalyticsUiState {
    object Idle : AnalyticsUiState()
    object Loading : AnalyticsUiState()
    data class Success(
        val data: AnalyticsResponse,
        val source: AnalyticsSource
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

@HiltViewModel
class GroqAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: GroqAnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Idle)
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    private var lastRequestKey: String? = null

    fun load(userId: String, listingId: String? = null) {
        if (userId.isBlank()) return

        val key = "$userId:${listingId.orEmpty()}"
        if (lastRequestKey == key && _state.value is AnalyticsUiState.Success) return
        lastRequestKey = key

        viewModelScope.launch {
            _state.value = AnalyticsUiState.Loading
            analyticsRepository.fetchAnalytics(userId, listingId)
                .onSuccess { result ->
                    _state.value = AnalyticsUiState.Success(
                        data = result.data,
                        source = result.source
                    )
                }
                .onFailure { _state.value = AnalyticsUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun reset() {
        lastRequestKey = null
        _state.value = AnalyticsUiState.Idle
    }
}
