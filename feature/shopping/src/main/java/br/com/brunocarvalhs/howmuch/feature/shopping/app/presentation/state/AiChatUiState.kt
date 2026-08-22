package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.ChatMessage

internal data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val input: String = ""
) : AiAgentContext {
    override fun toMetadata(): Map<String, Any?> = mapOf(
        "messages_count" to messages.size
    )
}
