package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ProductReader
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryBudget
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryInProgress
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryPurchaseEntry
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

private const val MONTHS_SHOWN = 6L

/**
 * Builds the month-by-month spend history shown on the "Histórico de gastos" screen: one
 * [SpendHistoryMonth] per calendar month, oldest first, covering the current month plus up to
 * [MONTHS_SHOWN] - 1 previous ones, clipped so it never starts before the account's earliest
 * list.
 */
internal class GetSpendHistoryUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productReader: ProductReader
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): List<SpendHistoryMonth> {
        val allLists = shoppingRepository.getAll()
        if (allLists.isEmpty()) return emptyList()

        val zoneId = ZoneId.systemDefault()
        val currentYearMonth = toYearMonth(now, zoneId)
        val months = visibleMonths(allLists, currentYearMonth, zoneId)

        val finishedLists = allLists.filter { it.status == Shopping.Status.FINISH }
        val inProgressLists = allLists.filter { it.status != Shopping.Status.FINISH }
        val candidateFinishedLists =
            finishedLists.filterRelevantTo(months, currentYearMonth, zoneId)

        val productsByShoppingId = loadProducts(candidateFinishedLists + inProgressLists)
        val resolvedPurchases = resolvePurchases(candidateFinishedLists, productsByShoppingId, zoneId)
        val inProgress = InProgressSummary.from(inProgressLists, productsByShoppingId)

        return months.map { yearMonth ->
            buildMonth(yearMonth, currentYearMonth, resolvedPurchases, inProgress)
        }
    }

    private fun visibleMonths(
        allLists: List<Shopping>,
        currentYearMonth: YearMonth,
        zoneId: ZoneId
    ): List<YearMonth> {
        val earliestYearMonth = allLists.minOf { toYearMonth(it.createdAt, zoneId) }
        val windowStart = currentYearMonth.minusMonths(MONTHS_SHOWN - 1)
        val effectiveStart = maxOf(earliestYearMonth, windowStart)

        return generateSequence(effectiveStart) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(currentYearMonth) }
            .toList()
    }

    // A finished list whose creation month is nowhere near the visible window can't resolve into
    // it either (the PURCHASED fallback never lands far from createdAt in practice), so this
    // skips the product read for it instead of paying for every finished list ever created.
    private fun List<Shopping>.filterRelevantTo(
        months: List<YearMonth>,
        currentYearMonth: YearMonth,
        zoneId: ZoneId
    ): List<Shopping> {
        val effectiveStart = months.firstOrNull() ?: currentYearMonth
        return filter { shopping ->
            val createdYearMonth = toYearMonth(shopping.createdAt, zoneId)
            !createdYearMonth.isBefore(effectiveStart.minusMonths(1)) &&
                !createdYearMonth.isAfter(currentYearMonth.plusMonths(1))
        }
    }

    private suspend fun loadProducts(lists: List<Shopping>): Map<String, List<Product>> =
        lists.associate { shopping -> shopping.id to productReader.getAllProducts(shopping.id).first() }

    private fun resolvePurchases(
        finishedLists: List<Shopping>,
        productsByShoppingId: Map<String, List<Product>>,
        zoneId: ZoneId
    ): List<ResolvedPurchase> = finishedLists.map { shopping ->
        val products = productsByShoppingId[shopping.id].orEmpty()
        val date = shopping.resolvedPurchaseDate(products)
        val total = shopping.resolvedPurchaseTotal(products)
        ResolvedPurchase(
            shopping = shopping,
            purchaseDate = date,
            total = total.amount,
            isEstimated = total.isEstimated,
            yearMonth = toYearMonth(date, zoneId)
        )
    }

    private fun buildMonth(
        yearMonth: YearMonth,
        currentYearMonth: YearMonth,
        resolvedPurchases: List<ResolvedPurchase>,
        inProgress: InProgressSummary
    ): SpendHistoryMonth {
        val purchasesOfMonth = resolvedPurchases.filter { it.yearMonth == yearMonth }
        val isCurrent = yearMonth == currentYearMonth
        val listsWithBudget = purchasesOfMonth.filter { it.shopping.budget != null }

        return SpendHistoryMonth(
            yearMonth = yearMonth,
            isCurrent = isCurrent,
            total = purchasesOfMonth.sumOf { it.total },
            budget = listsWithBudget.takeIf { it.isNotEmpty() }?.let { withBudget ->
                SpendHistoryBudget(
                    amount = withBudget.sumOf { it.shopping.budget ?: 0.0 },
                    spent = withBudget.sumOf { it.total }
                )
            },
            inProgress = if (isCurrent && inProgress.listCount > 0) {
                SpendHistoryInProgress(total = inProgress.total, listCount = inProgress.listCount)
            } else {
                null
            },
            purchases = purchasesOfMonth.map {
                SpendHistoryPurchaseEntry(
                    shopping = it.shopping,
                    purchaseDate = it.purchaseDate,
                    total = it.total,
                    isEstimated = it.isEstimated
                )
            }
        )
    }

    private data class ResolvedPurchase(
        val shopping: Shopping,
        val purchaseDate: Long,
        val total: Double,
        val isEstimated: Boolean,
        val yearMonth: YearMonth
    )

    // Purchased-item total/list-count still sitting in open lists, not bucketed by month: an
    // open list has no resolved purchase date yet, so it only ever surfaces on the current month.
    private data class InProgressSummary(val total: Double, val listCount: Int) {
        companion object {
            fun from(
                inProgressLists: List<Shopping>,
                productsByShoppingId: Map<String, List<Product>>
            ): InProgressSummary {
                val productsOf = { shopping: Shopping -> productsByShoppingId[shopping.id].orEmpty() }
                return InProgressSummary(
                    total = inProgressLists.sumOf { productsOf(it).purchasedTotal() },
                    listCount = inProgressLists.count { productsOf(it).any { product -> product.isPurchased } }
                )
            }
        }
    }
}

private fun List<Product>.purchasedTotal(): Double = filter { it.isPurchased }.sumOf { it.total }

private fun toYearMonth(epochMillis: Long, zoneId: ZoneId): YearMonth =
    YearMonth.from(Instant.ofEpochMilli(epochMillis).atZone(zoneId))
