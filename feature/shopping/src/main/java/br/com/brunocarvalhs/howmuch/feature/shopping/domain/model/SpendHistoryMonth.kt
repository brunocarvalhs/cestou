package br.com.brunocarvalhs.howmuch.feature.shopping.domain.model

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import java.time.YearMonth

/** One calendar month's worth of resolved spend data, as produced by `GetSpendHistoryUseCase`. */
internal data class SpendHistoryMonth(
    val yearMonth: YearMonth,
    val isCurrent: Boolean,
    val total: Double,
    val budget: SpendHistoryBudget?,
    val inProgress: SpendHistoryInProgress?,
    val purchases: List<SpendHistoryPurchaseEntry>
)

/** Sum of budgets vs. sum of resolved totals, over only the lists in the month that had a budget. */
internal data class SpendHistoryBudget(val amount: Double, val spent: Double)

/** Purchased-item total still sitting in open (non-finished) lists, current month only. */
internal data class SpendHistoryInProgress(val total: Double, val listCount: Int)

/** One resolved, finished purchase ("list") belonging to a [SpendHistoryMonth]. */
internal data class SpendHistoryPurchaseEntry(
    val shopping: Shopping,
    val purchaseDate: Long,
    val total: Double,
    val isEstimated: Boolean
)
