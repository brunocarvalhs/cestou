package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.InProgressSpend
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.MonthBudget
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.MonthSpendSummary

/**
 * Header for the selected month: name + total (bold, prominent), plus up to three detail rows.
 * Deliberately skips the reference credit-card screen's "due date" concept (no equivalent in a
 * grocery app) in favor of rows the spec grounded in this domain: purchase count, budget vs.
 * spend (only when at least one list in the month had a budget), and — current month only —
 * purchased items still sitting in open lists, kept visually separate from the closed total.
 */
@Composable
internal fun SpendHistorySummaryHeader(
    month: MonthSpendSummary,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = rememberCurrencyFormatter()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = month.fullLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currencyFormatter.format(month.total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (month.isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (month.isCurrent) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.spend_history_current_month_badge),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spend_history_purchase_count, month.purchaseCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        month.budget?.let { budget ->
            Text(
                text = stringResource(
                    R.string.spend_history_budget_line,
                    currencyFormatter.format(budget.amount),
                    currencyFormatter.format(budget.spent)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (budget.spent > budget.amount) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (month.isCurrent) {
            month.inProgress?.takeIf { it.listCount > 0 }?.let { inProgress ->
                Text(
                    text = stringResource(
                        R.string.spend_history_in_progress_line,
                        currencyFormatter.format(inProgress.total),
                        inProgress.listCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val previewMonth = MonthSpendSummary(
    id = "2026-06",
    monthLabel = "Jun",
    fullLabel = "Junho",
    total = 42.90,
    isCurrent = true,
    budget = MonthBudget(amount = 300.0, spent = 320.0),
    inProgress = InProgressSpend(total = 18.30, listCount = 1),
    purchases = StableList()
)

@Preview(showBackground = true)
@Composable
private fun SpendHistorySummaryHeaderPreview() {
    CestouTheme {
        SpendHistorySummaryHeader(month = previewMonth)
    }
}

@Preview(showBackground = true, name = "Mês passado, sem orçamento")
@Composable
private fun SpendHistorySummaryHeaderPastMonthPreview() {
    CestouTheme {
        SpendHistorySummaryHeader(
            month = previewMonth.copy(
                monthLabel = "Mai",
                fullLabel = "Maio",
                total = 76.12,
                isCurrent = false,
                budget = null,
                inProgress = null
            )
        )
    }
}
