package com.example.scrapsetu.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.DetectionUiState
import com.example.scrapsetu.data.repo.ImageDetectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageDetectionViewModel @Inject constructor(
    private val detectionRepository: ImageDetectionRepository
) : ViewModel() {

    private val _detectionState = MutableStateFlow<DetectionUiState>(DetectionUiState.Idle)
    val detectionState: StateFlow<DetectionUiState> = _detectionState.asStateFlow()

    private var detectionJob: Job? = null

    fun onImageSelected(uri: Uri) {
        detectionJob?.cancel()
        detectionJob = viewModelScope.launch {
            _detectionState.value = DetectionUiState.Analysing
            detectionRepository.detectAndPrice(uri)
                .onSuccess { _detectionState.value = DetectionUiState.Success(it) }
                .onFailure {
                    _detectionState.value = DetectionUiState.Error(
                        it.message ?: "Could not analyse image"
                    )
                }
        }
    }

    fun reset() {
        detectionJob?.cancel()
        _detectionState.value = DetectionUiState.Idle
    }
}
