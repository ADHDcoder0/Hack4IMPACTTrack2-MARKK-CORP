package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.User
import com.example.scrapsetu.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _usersById = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersById: StateFlow<Map<String, User>> = _usersById

    private var lastRequestKey: String? = null

    fun loadUsersByIds(ids: List<String>) {
        val distinctIds = ids.distinct().sorted()
        if (distinctIds.isEmpty()) {
            _usersById.value = emptyMap()
            lastRequestKey = null
            return
        }

        val requestKey = distinctIds.joinToString(",")
        if (requestKey == lastRequestKey) return

        viewModelScope.launch {
            try {
                _usersById.value = authRepository.getUsersByIds(distinctIds)
                lastRequestKey = requestKey
            } catch (_: Exception) {
                _usersById.value = emptyMap()
            }
        }
    }
}
