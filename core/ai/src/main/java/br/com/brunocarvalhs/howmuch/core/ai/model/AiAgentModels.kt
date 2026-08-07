package br.com.brunocarvalhs.howmuch.core.ai.model

import com.google.ai.client.generativeai.type.Content
import java.util.Locale

/**
 * Parâmetros que a IA deve fornecer para executar uma ação (Tool Calling).
 */
data class AiAgentParameter(
    val name: String,
    val type: String, // "string", "integer", "number", "boolean"
    val description: String,
    val isRequired: Boolean = true
)

/**
 * Dados de sessão que persistem durante as interações.
 */
data class AiAgentSession(
    val userId: String? = null,
    val locale: Locale = Locale.getDefault(),
    val history: MutableList<Content> = mutableListOf()
)
