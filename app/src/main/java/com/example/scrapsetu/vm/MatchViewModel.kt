package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.Match
import com.example.scrapsetu.data.repo.AuthRepository
import com.example.scrapsetu.data.repo.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MatchState {
    object Idle : MatchState()
    object Loading : MatchState()
    object Success : MatchState()
    data class Error(val message: String) : MatchState()
}

@HiltViewModel
class MatchViewModel @Inject constructor(
    private val matchRepo: MatchRepository,
    private val authRepo: AuthRepository
) : ViewModel() {
    private val _supplierMatches = MutableStateFlow<List<Match>>(emptyList())
    val supplierMatches: StateFlow<List<Match>> = _supplierMatches
    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches

    private val _matchState = MutableStateFlow<MatchState>(MatchState.Idle)
    val matchState: StateFlow<MatchState> = _matchState

    private var isLoadingMyMatches = false
    private var isLoadingSupplierMatches = false
    private var lastSupplierListingKey: String? = null

    fun requestMatch(listingId: String) {
        viewModelScope.launch {
            _matchState.value = MatchState.Loading
            try {
                val buyerId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                matchRepo.createMatch(listingId, buyerId)
                _matchState.value = MatchState.Success
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Match request failed")
            }
        }
    }

    fun loadMyMatches(forceRefresh: Boolean = false) {
        if (isLoadingMyMatches && !forceRefresh) return
        viewModelScope.launch {
            isLoadingMyMatches = true
            _matchState.value = MatchState.Loading
            try {
                val buyerId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                _matches.value = matchRepo.getBuyerMatches(buyerId)
                _matchState.value = MatchState.Idle
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Failed to load matches")
            } finally {
                isLoadingMyMatches = false
            }
        }
    }

    fun loadSupplierMatches(listingIds: List<String>, forceRefresh: Boolean = false) {
        if (isLoadingSupplierMatches && !forceRefresh) return

        val normalizedIds = listingIds.distinct().sorted()
        if (normalizedIds.isEmpty()) {
            _supplierMatches.value = emptyList()
            _matchState.value = MatchState.Idle
            lastSupplierListingKey = null
            return
        }

        val requestKey = normalizedIds.joinToString(",")
        if (!forceRefresh && requestKey == lastSupplierListingKey) return

        viewModelScope.launch {
            isLoadingSupplierMatches = true
            _matchState.value = MatchState.Loading
            try {
                _supplierMatches.value = matchRepo.getMatchesForSupplier(normalizedIds)
                lastSupplierListingKey = requestKey
                _matchState.value = MatchState.Idle
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Failed to load matches")
            } finally {
                isLoadingSupplierMatches = false
            }
        }
    }

    fun updateMatch(matchId: String, status: String) {
        viewModelScope.launch {
            try {
                matchRepo.updateMatchStatus(matchId, status)
                _matchState.value = MatchState.Success
                _supplierMatches.value = if (status.equals("rejected", ignoreCase = true)) {
                    _supplierMatches.value.filterNot { it.id == matchId }
                } else {
                    _supplierMatches.value.map {
                        if (it.id == matchId) it.copy(status = status) else it
                    }
                }
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Failed to update match")
            }
        }
    }

    fun revertMyRequest(matchId: String) {
        viewModelScope.launch {
            try {
                val buyerId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                matchRepo.deleteBuyerMatch(matchId, buyerId)
                _matches.value = _matches.value.filterNot { it.id == matchId }
                _matchState.value = MatchState.Success
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Failed to revert request")
            }
        }
    }
}