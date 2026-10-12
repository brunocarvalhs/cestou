package br.com.brunocarvalhs.howmuch.feature.chat.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage

interface ChatHistoryRepository {

    suspend fun load(shoppingId: String): List<ChatMessage>

    suspend fun save(shoppingId: String, messages: List<ChatMessage>)
}
