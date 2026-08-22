package br.com.brunocarvalhs.howmuch.feature.chat.presentation.state

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage

internal data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val input: String = ""
) : AiAgentContext {
    override fun toMetadata(): Map<String, Any?> = mapOf(
        "messages_count" to messages.size
    )
}
