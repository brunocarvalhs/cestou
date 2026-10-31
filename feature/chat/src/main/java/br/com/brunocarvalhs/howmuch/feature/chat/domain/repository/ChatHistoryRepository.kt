package br.com.brunocarvalhs.howmuch.feature.chat.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.chat.domain.model.ChatSession

interface ChatHistoryRepository {

    suspend fun getSessions(): List<ChatSession>

    suspend fun load(shoppingId: String): List<ChatMessage>

    suspend fun save(shoppingId: String, messages: List<ChatMessage>)

    suspend fun loadSession(sessionId: String): List<ChatMessage>

    suspend fun saveSession(
        sessionId: String,
        messages: List<ChatMessage>,
        shoppingId: String? = null,
        title: String? = null
    )

    suspend fun deleteSession(sessionId: String)

    suspend fun createNewSession(shoppingId: String? = null): ChatSession
}
