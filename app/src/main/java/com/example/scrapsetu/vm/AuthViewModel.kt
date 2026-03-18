package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    fun signUp(email: String, password: String, name: String, role: String, location: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepo.signUp(email, password, name, role, location)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepo.signIn(email, password)
                _authState.value = AuthState.Success
                android.util.Log.d("AuthViewModel", "Sign in SUCCESS")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Sign in FAILED: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepo.signOut()
            _authState.value = AuthState.Idle
        }
    }
    fun loadUserRole() {
        viewModelScope.launch {
            val user = authRepo.getCurrentUser()
            android.util.Log.d("AuthViewModel", "User role: ${user?.role}")
            _userRole.value = user?.role
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}