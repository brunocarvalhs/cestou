package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import com.google.ai.client.generativeai.type.Content
import java.util.Locale

/**
 * Dados de sessão que persistem durante as interações com a IA.
 * Esta implementação é específica para o Google Gemini.
 */
data class AgentSession(
    override val userId: String? = null,
    val locale: Locale = Locale.getDefault(),
    override val history: MutableList<Content> = mutableListOf()
) : AiSession {

    // Fora do construtor de propósito: é um sinal transiente por tentativa de sendMessage, não
    // um dado de identidade da sessão — não deve entrar em equals/hashCode/copy (sdd-012).
    override var toolExecutedThisTurn: Boolean = false
    override var providerUsed: String? = null
    override var toolCallsThisTurn: Int = 0
    override var fallbackUsedThisTurn: Boolean = false

    override fun clearHistory() {
        history.clear()
    }
}
