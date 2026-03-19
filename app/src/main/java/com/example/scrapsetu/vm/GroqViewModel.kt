package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.Listing
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
    }
}