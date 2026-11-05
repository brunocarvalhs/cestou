package br.com.brunocarvalhs.howmuch.core.domain.budget

/** Faixa de uso de um orçamento ou meta. */
enum class BudgetLevel { NONE, OK, WARNING, EXCEEDED }

/**
 * Quanto de um limite (orçamento da lista ou meta do mês) já foi usado. Um limite ausente ou
 * menor ou igual a zero não conta: sem limite não há progresso nem aviso.
 */
data class BudgetProgress(val limit: Double?, val spent: Double) {

    private val validLimit: Double? get() = limit?.takeIf { it > 0.0 }

    /** `spent / limit`, ou `null` sem limite válido. Pode passar de 1.0. */
    val ratio: Double? get() = validLimit?.let { spent / it }

    /** `limit - spent`, negativo quando estourou, ou `null` sem limite válido. */
    val remaining: Double? get() = validLimit?.let { it - spent }

    val level: BudgetLevel
        get() {
            val current = ratio ?: return BudgetLevel.NONE
            return when {
                current >= EXCEEDED_RATIO - EPSILON -> BudgetLevel.EXCEEDED
                current >= WARNING_RATIO - EPSILON -> BudgetLevel.WARNING
                else -> BudgetLevel.OK
            }
        }

    companion object {
        const val WARNING_RATIO = 0.8
        const val EXCEEDED_RATIO = 1.0

        /** Tolerância para somas de preços em ponto flutuante não deixarem 80% virar 79,999...%. */
        private const val EPSILON = 1e-9
    }
}
