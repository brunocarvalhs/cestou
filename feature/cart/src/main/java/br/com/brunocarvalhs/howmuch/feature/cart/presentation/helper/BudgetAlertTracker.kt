package br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper

import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.BudgetAlert
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.BudgetScope

/**
 * Decide quando avisar o gasto. A primeira leitura completa só fixa a linha de base: abrir uma
 * lista que já está em 90% não dispara aviso, a linha de progresso já mostra isso. Depois, cada
 * faixa (aviso e estouro) avisa uma única vez por abertura da lista e por escopo; a faixa
 * guardada nunca desce, então ficar entrando e saindo de 80% não repete o aviso.
 */
internal class BudgetAlertTracker {

    private var baselineSet = false
    private val notified = mutableMapOf(BudgetScope.LIST to BudgetLevel.NONE, BudgetScope.MONTH to BudgetLevel.NONE)

    /**
     * [ready] é verdadeiro quando produtos, outras listas e meta já chegaram; antes disso os
     * totais ainda estão incompletos e qualquer aviso seria falso.
     */
    fun onUpdate(ready: Boolean, list: BudgetProgress?, month: BudgetProgress?): BudgetAlert? {
        if (!ready) return null
        val levels = mapOf(
            BudgetScope.LIST to (list?.level ?: BudgetLevel.NONE),
            BudgetScope.MONTH to (month?.level ?: BudgetLevel.NONE)
        )
        val firstRead = !baselineSet
        baselineSet = true
        return if (firstRead) {
            levels.forEach { (scope, level) -> notified[scope] = level }
            null
        } else {
            crossedAlert(levels)
        }
    }

    /** Um aviso por vez: o estouro vale mais que o aviso, e a lista vem antes do mês. */
    private fun crossedAlert(levels: Map<BudgetScope, BudgetLevel>): BudgetAlert? {
        val crossed = levels.mapNotNull { (scope, level) ->
            val previous = notified.getValue(scope)
            if (level.isAlert() && level.ordinal > previous.ordinal) {
                notified[scope] = level
                BudgetAlert(scope, level)
            } else {
                null
            }
        }
        return crossed.maxWithOrNull(compareBy<BudgetAlert> { it.level.ordinal }.thenBy { -it.scope.ordinal })
    }

    private fun BudgetLevel.isAlert() = this == BudgetLevel.WARNING || this == BudgetLevel.EXCEEDED
}
