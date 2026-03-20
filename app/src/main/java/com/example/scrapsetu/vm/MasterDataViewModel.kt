package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.IndiaDistrict
import com.example.scrapsetu.data.model.IndiaState
import com.example.scrapsetu.data.model.WasteCategory
import com.example.scrapsetu.data.repo.MasterDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MasterDataViewModel @Inject constructor(
    private val masterDataRepository: MasterDataRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<WasteCategory>>(emptyList())
    val categories: StateFlow<List<WasteCategory>> = _categories

    private val _states = MutableStateFlow<List<IndiaState>>(emptyList())
    val states: StateFlow<List<IndiaState>> = _states

    private val _districts = MutableStateFlow<List<IndiaDistrict>>(emptyList())
    val districts: StateFlow<List<IndiaDistrict>> = _districts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadInitialData() {
        if (_categories.value.isNotEmpty() && _states.value.isNotEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _categories.value = masterDataRepository.getWasteCategories()
                _states.value = masterDataRepository.getIndiaStates()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load category and location options"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDistricts(stateCode: String) {
        if (stateCode.isBlank()) {
            _districts.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _districts.value = masterDataRepository.getDistrictsByState(stateCode)
            } catch (e: Exception) {
                _districts.value = emptyList()
                _errorMessage.value = e.message ?: "Failed to load districts"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
