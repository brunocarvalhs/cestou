package br.com.brunocarvalhs.howmuch.feature.chat.domain.model

data class ChatSession(
    val id: String,
    val title: String,
    val shoppingId: String? = null,
    val lastMessage: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
)
