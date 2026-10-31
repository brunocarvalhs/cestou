package br.com.brunocarvalhs.howmuch.feature.chat.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ChatSessionModel(
    val id: String,
    val title: String,
    val shoppingId: String? = null,
    val lastMessage: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
)

@Serializable
internal data class ChatSessionsIndexModel(
    val sessions: List<ChatSessionModel> = emptyList(),
)
