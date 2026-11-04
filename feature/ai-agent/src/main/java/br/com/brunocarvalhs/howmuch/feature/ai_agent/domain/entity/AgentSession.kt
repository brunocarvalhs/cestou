package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.AiHistoryTurn
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _activeAction = MutableStateFlow<String?>(null)
    override val activeAction: StateFlow<String?> = _activeAction.asStateFlow()

    override fun setActiveAction(id: String?) {
        _activeAction.value = id
    }

    private var turnIndex = 0
    private val confirmationRequests = mutableMapOf<String, Int>()

    override fun clearHistory() {
        history.clear()
        confirmationRequests.clear()
    }

    /**
     * Falas seguidas do mesmo autor (ex. um erro gravado como resposta) viram uma só: o Gemini
     * espera user/model alternados.
     */
    override fun restoreHistory(turns: List<AiHistoryTurn>) {
        history.clear()
        confirmationRequests.clear()
        turns.filter { it.text.isNotBlank() }.forEach { turn ->
            val role = if (turn.fromUser) "user" else "model"
            val last = history.lastOrNull()
            if (last?.role == role) {
                val merged = (last.parts.filterIsInstance<TextPart>().joinToString("\n") { it.text }) + "\n" + turn.text
                history[history.lastIndex] = Content(role = role, parts = listOf(TextPart(merged)))
            } else {
                history.add(Content(role = role, parts = listOf(TextPart(turn.text))))
            }
        }
        history.trimToRecentHistory()
    }

    override fun beginTurn() {
        turnIndex++
    }

    override fun requestConfirmation(key: String) {
        confirmationRequests[key] = turnIndex
    }

    override fun consumeConfirmation(key: String): Boolean {
        val approved = (confirmationRequests[key] ?: turnIndex) < turnIndex
        if (approved) confirmationRequests.remove(key)
        return approved
    }
}
