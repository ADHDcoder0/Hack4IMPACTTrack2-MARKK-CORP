package com.example.scrapsetu.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrapsetu.data.model.ChatUiState
import com.example.scrapsetu.data.model.MESSAGE_LIMIT
import com.example.scrapsetu.data.model.Message
import com.example.scrapsetu.data.model.SendState
import com.example.scrapsetu.data.repo.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private var currentMatchId: String = ""
    private var currentUserId: String = ""
    private var subscriptionJob: Job? = null

    fun openChat(matchId: String, currentUserId: String) {
        if (matchId.isBlank() || currentUserId.isBlank()) return
        if (this.currentMatchId == matchId && this.currentUserId == currentUserId) return

        this.currentMatchId = matchId
        this.currentUserId = currentUserId

        viewModelScope.launch {
            _state.value = ChatUiState.Loading

            chatRepository.purgeExpiredMessages(matchId)

            val messages = runCatching { chatRepository.loadMessages(matchId) }
                .getOrElse {
                    _state.value = ChatUiState.Error(it.message ?: "Failed to load chat")
                    return@launch
                }

            _messages.value = messages
            emitReady()

            chatRepository.markAllRead(matchId, currentUserId)

            subscriptionJob?.cancel()
            subscriptionJob = chatRepository.subscribeToMessages(matchId)
                .onEach { newMsg ->
                    _messages.update { current ->
                        if (current.any { it.id == newMsg.id }) current
                        else (current + newMsg).takeLast(MESSAGE_LIMIT)
                    }
                    emitReady()
                    if (newMsg.senderId != currentUserId) {
                        chatRepository.markAllRead(matchId, currentUserId)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || currentMatchId.isEmpty() || isLimitReached()) return

        viewModelScope.launch {
            _sendState.value = SendState.Sending
            chatRepository.sendText(currentMatchId, currentUserId, trimmed)
                .onSuccess { sentMessage ->
                    appendMessageIfMissing(sentMessage)
                    _sendState.value = SendState.Sent
                    _sendState.value = SendState.Idle
                }
                .onFailure {
                    _sendState.value = SendState.Error(it.message ?: "Send failed")
                }
        }
    }

    private fun appendMessageIfMissing(newMsg: Message) {
        _messages.update { current ->
            if (current.any { it.id == newMsg.id }) current
            else (current + newMsg).takeLast(MESSAGE_LIMIT)
        }
        emitReady()
    }

    private fun isLimitReached(): Boolean = _messages.value.size >= MESSAGE_LIMIT

    private fun emitReady() {
        _state.value = ChatUiState.Ready(
            messages = _messages.value,
            limitReached = isLimitReached(),
            currentUserId = currentUserId
        )
    }

    override fun onCleared() {
        subscriptionJob?.cancel()
        super.onCleared()
    }
}
