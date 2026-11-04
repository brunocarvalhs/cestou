package br.com.brunocarvalhs.howmuch.core.ai.model

/** Tempo que um pedido de aprovação espera pelo usuário antes de valer como recusa. */
const val APPROVAL_TIMEOUT_MS = 120_000L

/**
 * Pedido de aprovação de uma ação irreversível da IA, mostrado ao usuário num card.
 * [summary] diz o que será afetado (ex. o título da lista); é dado do usuário, sem tradução.
 */
data class ApprovalRequest(
    val actionId: String,
    val summary: String?
)

/** Como um pedido de aprovação terminou. Só [APPROVED] deixa a ação executar. */
enum class ApprovalDecision {
    APPROVED,
    REJECTED,

    /** O usuário não respondeu a tempo. */
    TIMEOUT,

    /** O usuário saiu do chat com o card aberto. */
    DISMISSED;

    val isApproved: Boolean get() = this == APPROVED
}
