package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ObserveHistoryUseCase @Inject constructor(
    private val repository: ShoppingRepository,
    private val groupByMonth: GroupHistoryByMonthUseCase
) {

    operator fun invoke(): Flow<List<HistoryMonth>> = repository.observeAll()
        .map(groupByMonth::invoke)
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}
