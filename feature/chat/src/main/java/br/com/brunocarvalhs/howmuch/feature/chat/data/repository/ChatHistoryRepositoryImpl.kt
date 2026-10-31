package br.com.brunocarvalhs.howmuch.feature.chat.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.get
import br.com.brunocarvalhs.howmuch.feature.chat.data.mapper.toDomain
import br.com.brunocarvalhs.howmuch.feature.chat.data.mapper.toModel
import br.com.brunocarvalhs.howmuch.feature.chat.data.model.ChatHistoryModel
import br.com.brunocarvalhs.howmuch.feature.chat.data.model.ChatSessionsIndexModel
import br.com.brunocarvalhs.howmuch.feature.chat.di.ChatDataStore
import br.com.brunocarvalhs.howmuch.feature.chat.domain.model.ChatSession
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import java.util.UUID
import javax.inject.Inject

internal class ChatHistoryRepositoryImpl @Inject constructor(
    @ChatDataStore private val storageService: StorageService,
) : ChatHistoryRepository {

    override suspend fun getSessions(): List<ChatSession> {
        val index = storageService.get<ChatSessionsIndexModel>(KEY_SESSIONS_INDEX)
            ?: return emptyList()
        return index.sessions.map { it.toDomain() }
    }

    override suspend fun load(shoppingId: String): List<ChatMessage> {
        val sessions = getSessions()
        val sessionForShopping = sessions.firstOrNull { it.shoppingId == shoppingId }
        val keyToLoad = sessionForShopping?.let { key(it.id) } ?: legacyKey(shoppingId)

        return storageService.get<ChatHistoryModel>(keyToLoad)
            ?.messages
            ?.mapNotNull { it.toDomain() }
            ?: emptyList()
    }

    override suspend fun save(shoppingId: String, messages: List<ChatMessage>) {
        val sessions = getSessions()
        val existingSession = sessions.firstOrNull { it.shoppingId == shoppingId }
        val sessionId = existingSession?.id ?: shoppingId

        saveSession(
            sessionId = sessionId,
            messages = messages,
            shoppingId = shoppingId,
            title = existingSession?.title,
        )
    }

    override suspend fun loadSession(sessionId: String): List<ChatMessage> {
        return storageService.get<ChatHistoryModel>(key(sessionId))
            ?.messages
            ?.mapNotNull { it.toDomain() }
            ?: emptyList()
    }

    override suspend fun saveSession(
        sessionId: String,
        messages: List<ChatMessage>,
        shoppingId: String?,
        title: String?,
    ) {
        val historyModel = ChatHistoryModel(messages.map { it.toModel() })
        storageService.save(key(sessionId), historyModel)

        val sessions = getSessions().toMutableList()
        val lastUserMessage = messages.lastOrNull { it.sender == ChatMessage.Sender.USER }?.text
            ?: messages.lastOrNull()?.text ?: ""

        val existingIndex = sessions.indexOfFirst { it.id == sessionId }
        val resolvedTitle = title ?: existingIndex.takeIf { it != -1 }?.let { sessions[it].title }
            ?: deriveTitle(lastUserMessage)

        val updatedSession = ChatSession(
            id = sessionId,
            title = resolvedTitle,
            shoppingId = shoppingId ?: (if (existingIndex != -1) sessions[existingIndex].shoppingId else null),
            lastMessage = lastUserMessage,
            updatedAtEpochMillis = System.currentTimeMillis(),
            messageCount = messages.size,
        )

        if (existingIndex != -1) {
            sessions[existingIndex] = updatedSession
        } else {
            sessions.add(0, updatedSession)
        }

        sessions.sortByDescending { it.updatedAtEpochMillis }
        storageService.save(
            KEY_SESSIONS_INDEX,
            ChatSessionsIndexModel(sessions.map { it.toModel() }),
        )
    }

    override suspend fun deleteSession(sessionId: String) {
        val sessions = getSessions().filterNot { it.id == sessionId }
        storageService.save(
            KEY_SESSIONS_INDEX,
            ChatSessionsIndexModel(sessions.map { it.toModel() }),
        )
        storageService.remove(key(sessionId))
    }

    override suspend fun createNewSession(shoppingId: String?): ChatSession {
        val newSessionId = UUID.randomUUID().toString()
        val newSession = ChatSession(
            id = newSessionId,
            title = "Nova conversa",
            shoppingId = shoppingId,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        val sessions = getSessions().toMutableList()
        sessions.add(0, newSession)
        storageService.save(
            KEY_SESSIONS_INDEX,
            ChatSessionsIndexModel(sessions.map { it.toModel() }),
        )
        return newSession
    }

    private fun deriveTitle(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return "Nova conversa"
        return if (trimmed.length > TITLE_MAX_LENGTH) {
            "${trimmed.take(TITLE_MAX_LENGTH)}…"
        } else {
            trimmed
        }
    }

    private fun key(sessionId: String) = "$KEY_PREFIX$sessionId"
    private fun legacyKey(shoppingId: String) = "$LEGACY_KEY_PREFIX$shoppingId"

    companion object {
        private const val KEY_SESSIONS_INDEX = "chat_sessions_index"
        private const val KEY_PREFIX = "chat_session_"
        private const val LEGACY_KEY_PREFIX = "chat_history_"
        private const val TITLE_MAX_LENGTH = 30
    }
}
