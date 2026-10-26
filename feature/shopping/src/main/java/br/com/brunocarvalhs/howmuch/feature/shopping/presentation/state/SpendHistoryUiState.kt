package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

/**
 * UI state for the "Histórico de gastos" (spend history) screen.
 *
 * Field names deliberately don't mirror `core/domain`'s `Shopping`/`Product` 1:1: the ViewModel
 * maps whatever the resolved purchase data (date/total fallback chain included) looks like onto
 * this shape, so the Composables never need to know about those fallbacks.
 */
@Stable
internal data class SpendHistoryUiState(
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val months: StableList<MonthSpendSummary> = StableList(),
    val selectedMonthId: String? = null
)

/**
 * One month card, plus everything needed to render it selected (summary header + purchase
 * list). [isCurrent] drives the "parcial" styling (outlined card, open marker, accent total) —
 * the in-progress month, per the reference layout.
 */
@Immutable
internal data class MonthSpendSummary(
    val id: String,
    val monthLabel: String,
    val fullLabel: String,
    val total: Double,
    val isCurrent: Boolean = false,
    val budget: MonthBudget? = null,
    val inProgress: InProgressSpend? = null,
    val purchases: StableList<SpendHistoryPurchase> = StableList()
) {
    val purchaseCount: Int get() = purchases.size
}

/** Sum of budgets vs. sum of paid totals, over only the lists in the month that had a budget. */
@Immutable
internal data class MonthBudget(val amount: Double, val spent: Double)

/** Purchased-item total still sitting in open (non-finished) lists — current month only. */
@Immutable
internal data class InProgressSpend(val total: Double, val listCount: Int)

/** One finished shopping list ("purchase") — the grain this screen renders, not per-item. */
@Immutable
internal data class SpendHistoryPurchase(
    val id: String,
    val timestamp: Long,
    val emoji: String?,
    val title: String,
    val establishment: String?,
    val total: Double,
    val isEstimated: Boolean = false,
    val isShared: Boolean = false
)
