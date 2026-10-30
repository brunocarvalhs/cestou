package br.com.brunocarvalhs.howmuch.feature.chat.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage

@Stable
data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val input: String = "",
    val shoppingId: String? = null,
    val showHistory: Boolean = false,
    val previousConversations: List<String> = emptyList()
) : AiAgentContext {
    override fun toMetadata(): Map<String, Any?> = mapOf(
        "messages_count" to messages.size,
        "shopping_id" to shoppingId
    )
}
