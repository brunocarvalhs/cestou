package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryPurchase
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject

/**
 * Groups finished lists by the month they were bought in. Months come out oldest first (left to
 * right in the carousel) and purchases newest first inside each month. Only months with at least
 * one purchase exist.
 */
internal class GroupHistoryByMonthUseCase @Inject constructor(
    private val clock: Clock
) {

    operator fun invoke(lists: List<Shopping>): List<HistoryMonth> = lists
        .filter { it.status == Shopping.Status.FINISH }
        .map { it.toPurchase() }
        .groupBy { YearMonth.from(Instant.ofEpochMilli(it.date).atZone(clock.zone)) }
        .map { (month, purchases) ->
            HistoryMonth(
                key = month,
                total = purchases.sumOf { it.total },
                purchases = purchases.sortedByDescending { it.date }
            )
        }
        .sortedBy { it.key }

    private fun Shopping.toPurchase() = HistoryPurchase(
        shopping = this,
        date = finishedAt ?: createdAt,
        establishment = establishment?.takeIf { it.isNotBlank() },
        peopleCount = users.size.coerceAtLeast(1),
        total = price
    )
}
