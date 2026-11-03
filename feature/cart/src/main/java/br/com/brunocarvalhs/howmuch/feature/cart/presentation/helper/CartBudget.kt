package br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper

import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.domain.budget.MonthlySpendingCalculator
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList

/** O que o orçamento da lista depende: a lista, os itens e as outras listas do usuário. */
internal data class BudgetInputs(
    val shopping: Shopping?,
    val products: StableList<Product>,
    val others: StableList<Shopping>
)

/** Orçamento da lista e projeção do mês; cada um é `null` quando não há limite válido. */
internal data class BudgetSnapshot(val list: BudgetProgress?, val month: BudgetProgress?)

/**
 * Total da lista contra o orçamento dela e, com meta, a projeção do mês. Lista aberta entra no mês
 * pelo total dos itens ("se finalizar agora"); uma já finalizada entra pelo que foi pago, e só se
 * a compra caiu neste mês.
 */
internal fun computeCartBudget(
    inputs: BudgetInputs,
    goal: Double?,
    calculator: MonthlySpendingCalculator
): BudgetSnapshot {
    val total = inputs.products.items.sumOf { it.total }
    val list = BudgetProgress(limit = inputs.shopping?.budget, spent = total)
        .takeIf { it.level != BudgetLevel.NONE }
    val month = goal?.let { limit ->
        val finished = inputs.shopping?.takeIf { it.status == Shopping.Status.FINISH }
        val spent = if (finished != null) {
            calculator.finishedSpent(listOf(finished) + inputs.others.items)
        } else {
            calculator.finishedSpent(inputs.others.items) + total
        }
        BudgetProgress(limit = limit, spent = spent)
    }
    return BudgetSnapshot(list, month)
}
