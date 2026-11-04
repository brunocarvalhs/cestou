package br.com.brunocarvalhs.howmuch.feature.cart.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel

/** O que está sob alerta: o orçamento da lista ou a meta do mês. */
internal enum class BudgetScope { LIST, MONTH }

/** Aviso de gasto a mostrar uma vez, ao cruzar 80% ou 100% de [scope]. */
internal data class BudgetAlert(val scope: BudgetScope, val level: BudgetLevel)
