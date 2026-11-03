package br.com.brunocarvalhs.howmuch.core.ai.contract

import br.com.brunocarvalhs.howmuch.core.ai.model.APPROVAL_TIMEOUT_MS
import br.com.brunocarvalhs.howmuch.core.ai.model.AiHistoryTurn
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalRequest
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface marcadora para a sessão do Agente de IA.
 * Implementações concretas residem no módulo de orquestração (:feature:ai-agent).
 */
interface AiSession {
    val userId: String?
    val history: MutableList<Content>

    /**
     * Limpa [history] sem expor o tipo [Content] (do SDK do Gemini, dependência `implementation`
     * de `:core:ai`) a módulos que só precisam encerrar a sessão — ex. `AiChatViewModel` ao trocar
     * de lista e `MainViewModel` no logout (sdd-006, B-11) — e que por isso não têm esse SDK no
     * classpath de compilação.
     */
    fun clearHistory()

    /**
     * `id` da ação que a IA está executando agora, ou `null`. Só alimenta o texto de andamento da
     * tela; não entra em [history] nem no histórico gravado do chat.
     */
    val activeAction: StateFlow<String?>

    fun setActiveAction(id: String?)

    /**
     * Substitui [history] pelos [turns] de uma conversa já gravada, para a IA lembrar do que foi
     * dito ao reabrir um chat. Recebe um tipo próprio porque [Content] não está no classpath de
     * quem carrega o histórico (`:feature:chat`).
     */
    fun restoreHistory(turns: List<AiHistoryTurn>)

    /** Pedido de aprovação aberto, ou `null`. É o que o chat mostra como card. */
    val pendingApproval: StateFlow<ApprovalRequest?>

    /**
     * Publica [request] e suspende até [resolveApproval] ou o fim de [timeoutMillis]. Só um
     * pedido por vez: um segundo espera o primeiro terminar. Sempre limpa o pedido ao sair,
     * inclusive no cancelamento do turno.
     */
    suspend fun awaitApproval(request: ApprovalRequest, timeoutMillis: Long = APPROVAL_TIMEOUT_MS): ApprovalDecision

    /** Entrega a decisão do usuário ao pedido aberto; sem pedido aberto, não faz nada. */
    fun resolveApproval(decision: ApprovalDecision)

    /**
     * `true` quando uma tool com efeito colateral real (ex. produto gravado no Firestore) já foi
     * despachada na tentativa de `sendMessage` em curso. [FallbackAgent] lê este sinal antes de
     * tentar o provider secundário: sem ele, uma falha de transporte *depois* da tool já ter
     * rodado faria o fallback reexecutar o pedido inteiro no outro provider, duplicando a tool
     * (sdd-012). Cada agente concreto zera este sinal no início do próprio `sendMessage`.
     */
    var toolExecutedThisTurn: Boolean

    /**
     * Tag do provider (`"gemini"`, `"openrouter"`) que atendeu a tentativa de `sendMessage` mais
     * recente; só telemetria, para saber qual agente o cliente realmente usa.
     */
    var providerUsed: String?

    /** Quantas tools foram despachadas na tentativa em curso (zerado no início de cada tentativa). */
    var toolCallsThisTurn: Int

    /** `true` quando o provider secundário respondeu no lugar do primário; quem envia zera antes. */
    var fallbackUsedThisTurn: Boolean
}
