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

    fun loadMyMatches() {
        viewModelScope.launch {
            _matchState.value = MatchState.Loading
            try {
                val buyerId = authRepo.currentUserId() ?: throw Exception("Not logged in")
                _matches.value = matchRepo.getBuyerMatches(buyerId)
                _matchState.value = MatchState.Idle
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Failed to load matches")
            }
        }
    }

    fun loadSupplierMatches(listingIds: List<String>) {
        viewModelScope.launch {
            _matchState.value = MatchState.Loading
            try {
                _supplierMatches.value = matchRepo.getMatchesForSupplier(listingIds)
                _matchState.value = MatchState.Idle
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Failed to load matches")
            }
        }
    }

    fun updateMatch(matchId: String, status: String) {
        viewModelScope.launch {
            try {
                matchRepo.updateMatchStatus(matchId, status)
                // refresh after update
                _supplierMatches.value = _supplierMatches.value.map {
                    if (it.id == matchId) it.copy(status = status) else it
                }
            } catch (e: Exception) {
                _matchState.value = MatchState.Error(e.message ?: "Failed to update match")
            }
        }
    }
}