package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.CategorySpending
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Named

private const val MAX_PARALLEL_READS = 4

/** `@Named` key of the dispatcher used for product reads, provided in `ShoppingModule`. */
internal const val SPENDING_IO_DISPATCHER = "shopping_spending_io"

/**
 * Sums the purchased products of [purchases] by raw category key. Each list costs one read of its
 * products (single snapshot, no listener), so reads run at most [MAX_PARALLEL_READS] at a time and
 * finished lists are served from [CategorySpendingCache]. If any list fails the whole call fails,
 * because a partial ranking would silently under-report.
 */
internal class GetCategorySpendingUseCase @Inject constructor(
    private val productsUseCase: ProductsUseCase,
    private val cache: CategorySpendingCache,
    @Named(SPENDING_IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(purchases: List<Shopping>): Result<List<CategorySpending>> = runCatching {
        val semaphore = Semaphore(MAX_PARALLEL_READS)
        val perList = coroutineScope {
            purchases.map { shopping ->
                async(ioDispatcher) { semaphore.withPermit { totalsOf(shopping) } }
            }.map { it.await() }
        }
        merge(perList)
    }.onFailure { if (it is CancellationException) throw it }

    private suspend fun totalsOf(shopping: Shopping): Map<String, Double> {
        cache.get(shopping.id, shopping.finishedAt)?.let { return it }
        val totals = productsUseCase(shopping.id).first()
            .filter { it.isPurchased }
            .groupBy { it.category.trim().lowercase() }
            .mapValues { (_, products) -> products.sumOf { it.total } }
        cache.put(shopping.id, shopping.finishedAt, totals)
        return totals
    }

    private fun merge(perList: List<Map<String, Double>>): List<CategorySpending> = perList
        .flatMap { it.entries }
        .groupBy({ it.key }, { it.value })
        .map { (key, values) -> CategorySpending(key, values.sum()) }
        .filter { it.total > 0.0 }
}
