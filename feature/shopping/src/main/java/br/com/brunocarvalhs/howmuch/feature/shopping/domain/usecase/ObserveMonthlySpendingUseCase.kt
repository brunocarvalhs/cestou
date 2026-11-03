package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val DEFAULT_MONTH_LIMIT = 6

internal class ObserveMonthlySpendingUseCase @Inject constructor(
    private val observeHistory: ObserveHistoryUseCase
) {

    /** The [limit] most recent months with purchases, oldest first. */
    operator fun invoke(limit: Int = DEFAULT_MONTH_LIMIT): Flow<List<HistoryMonth>> =
        observeHistory().map { it.takeLast(limit) }
}
