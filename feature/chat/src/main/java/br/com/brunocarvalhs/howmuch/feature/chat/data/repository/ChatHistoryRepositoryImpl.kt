package br.com.brunocarvalhs.howmuch.feature.chat.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.get
import br.com.brunocarvalhs.howmuch.feature.chat.data.mapper.toDomain
import br.com.brunocarvalhs.howmuch.feature.chat.data.mapper.toModel
import br.com.brunocarvalhs.howmuch.feature.chat.data.model.ChatHistoryModel
import br.com.brunocarvalhs.howmuch.feature.chat.di.ChatDataStore
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import javax.inject.Inject

internal class ChatHistoryRepositoryImpl @Inject constructor(
    @ChatDataStore private val storageService: StorageService
) : ChatHistoryRepository {

    override suspend fun load(shoppingId: String): List<ChatMessage> =
        storageService.get<ChatHistoryModel>(key(shoppingId))
            ?.messages
            ?.map { it.toDomain() }
            ?: emptyList()

    override suspend fun save(shoppingId: String, messages: List<ChatMessage>) {
        storageService.save(key(shoppingId), ChatHistoryModel(messages.map { it.toModel() }))
    }

    private fun key(shoppingId: String) = "$KEY_PREFIX$shoppingId"

    companion object {
        private const val KEY_PREFIX = "chat_history_"
    }
}
