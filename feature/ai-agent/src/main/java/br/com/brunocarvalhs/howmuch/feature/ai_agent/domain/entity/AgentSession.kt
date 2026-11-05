package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.AiHistoryTurn
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalRequest
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
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

    private val _pendingApproval = MutableStateFlow<ApprovalRequest?>(null)
    override val pendingApproval: StateFlow<ApprovalRequest?> = _pendingApproval.asStateFlow()

    // Um pedido por vez: um segundo awaitApproval espera o primeiro terminar.
    private val approvalLock = Mutex()

    @Volatile
    private var decision: CompletableDeferred<ApprovalDecision>? = null

    override fun clearHistory() {
        history.clear()
    }

    /**
     * Falas seguidas do mesmo autor (ex. um erro gravado como resposta) viram uma só: o Gemini
     * espera user/model alternados.
     */
    override fun restoreHistory(turns: List<AiHistoryTurn>) {
        history.clear()
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

    override suspend fun awaitApproval(request: ApprovalRequest, timeoutMillis: Long): ApprovalDecision =
        approvalLock.withLock {
            val deferred = CompletableDeferred<ApprovalDecision>()
            decision = deferred
            _pendingApproval.value = request
            try {
                withTimeoutOrNull(timeoutMillis) { deferred.await() } ?: ApprovalDecision.TIMEOUT
            } finally {
                decision = null
                _pendingApproval.value = null
            }
        }

    override fun resolveApproval(decision: ApprovalDecision) {
        this.decision?.complete(decision)
    }
}
