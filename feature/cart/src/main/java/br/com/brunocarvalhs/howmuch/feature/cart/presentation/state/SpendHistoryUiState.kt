package br.com.brunocarvalhs.howmuch.feature.cart.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

/**
 * Presentation-only UI state for the "Histórico de gastos" (spend history) screen.
 *
 * Provisional module placement: `feature/cart` already owns "finish purchase" logic, which is
 * why this lives here for now, but `.specs/features/purchase-history-export/spec.md` (draft at
 * the time this was written) floats `feature/shopping` as the real home, next to the list it's
 * opened from. This file and the `spendhistory` components package intentionally don't depend on
 * [br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.CartUiState] or anything else
 * cart-specific, so relocating the whole package later is a file move, not a rewrite.
 *
 * Field names deliberately don't mirror `core/domain`'s `Shopping`/`Product` 1:1 — the data layer
 * isn't finalized (the spec notes `Shopping.finishedAt` doesn't exist yet and purchase-total
 * resolution has fallbacks). A future ViewModel maps whatever the real repository returns onto
 * this shape; until then, previews populate it with fake data.
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

/**
 * One finished shopping list ("purchase") — the grain this screen renders, per the spec's "one
 * purchase = one finished list" data-grain decision (not per-item).
 */
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
