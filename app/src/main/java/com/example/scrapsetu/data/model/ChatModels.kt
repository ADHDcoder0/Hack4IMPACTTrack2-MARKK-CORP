package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    @SerialName("match_id") val matchId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val content: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
) {
    val isRead: Boolean get() = readAt != null
}

const val MESSAGE_LIMIT = 50

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Ready(
        val messages: List<Message>,
        val limitReached: Boolean,
        val currentUserId: String
    ) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

sealed class SendState {
    object Idle : SendState()
    object Sending : SendState()
    object Sent : SendState()
    data class Error(val msg: String) : SendState()
}
