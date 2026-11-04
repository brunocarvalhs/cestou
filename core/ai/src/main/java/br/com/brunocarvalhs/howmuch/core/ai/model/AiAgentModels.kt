package br.com.brunocarvalhs.howmuch.core.ai.model

/**
 * Parâmetros que a IA deve fornecer para executar uma ação (Tool Calling).
 */
data class AiAgentParameter(
    val name: String,
    val type: String, // "string", "integer", "number", "boolean"
    val description: String,
    val isRequired: Boolean = true
)

/** Uma fala de uma conversa gravada, na ordem em que aconteceu. */
data class AiHistoryTurn(
    val fromUser: Boolean,
    val text: String
)
