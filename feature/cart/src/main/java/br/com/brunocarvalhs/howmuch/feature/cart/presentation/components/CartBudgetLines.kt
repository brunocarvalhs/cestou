package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBudgetProgress
import br.com.brunocarvalhs.howmuch.core.ui.components.budgetListCaption
import br.com.brunocarvalhs.howmuch.core.ui.components.budgetMonthCaption
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.BudgetAlert
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.BudgetScope

/**
 * Linhas de progresso sob o cabeçalho da lista: o total contra o orçamento da lista e, com meta
 * definida, o impacto no mês. Sem nenhum dos dois, não ocupa espaço.
 */
@Composable
internal fun CartBudgetLines(
    listProgress: BudgetProgress?,
    monthProgress: BudgetProgress?,
    modifier: Modifier = Modifier
) {
    if (listProgress == null && monthProgress == null) return
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listProgress?.let {
            CestouBudgetProgress(
                progress = it,
                caption = budgetListCaption(it),
                modifier = Modifier.testTag("cart_budget_progress")
            )
        }
        monthProgress?.let {
            CestouBudgetProgress(
                progress = it,
                caption = budgetMonthCaption(it),
                modifier = Modifier.testTag("cart_month_progress")
            )
        }
    }
}

/** Texto do aviso ao cruzar 80% ou 100% do orçamento da lista ou da meta do mês. */
@Composable
internal fun budgetAlertText(alert: BudgetAlert): String {
    val exceeded = alert.level == BudgetLevel.EXCEEDED
    val res = when (alert.scope) {
        BudgetScope.LIST ->
            if (exceeded) R.string.cart_budget_alert_list_exceeded else R.string.cart_budget_alert_list_warning
        BudgetScope.MONTH ->
            if (exceeded) R.string.cart_budget_alert_month_exceeded else R.string.cart_budget_alert_month_warning
    }
    return stringResource(res)
}
