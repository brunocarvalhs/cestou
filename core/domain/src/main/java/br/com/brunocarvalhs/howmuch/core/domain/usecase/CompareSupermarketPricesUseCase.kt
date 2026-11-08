package br.com.brunocarvalhs.howmuch.core.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.ChainQuote
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketPriceComparison
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.domain.repository.SupermarketRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Estima quanto [products] custariam em cada rede da praça e ordena da mais barata para a mais
 * cara. Só itens com preço entram na conta; os demais voltam em `unpricedCount`. O cálculo roda
 * em [dispatcher] para não ocupar a main thread com listas grandes.
 */
class CompareSupermarketPricesUseCase(
    private val repository: SupermarketRepository,
    private val dispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(repository: SupermarketRepository) : this(repository, Dispatchers.Default)

    suspend operator fun invoke(products: List<Product>, location: UserLocation): SupermarketPriceComparison =
        withContext(dispatcher) {
            val priced = products.filter { (it.price ?: 0.0) > 0.0 }
            val baseTotal = priced.sumOf { it.total }
            val totals = repository.getChainIndexes(location)
                .map { it.chain to baseTotal * it.index }
                .sortedBy { (_, total) -> total }
            val mostExpensive = totals.lastOrNull()?.second ?: 0.0
            SupermarketPriceComparison(
                location = location,
                baseTotal = baseTotal,
                unpricedCount = products.size - priced.size,
                quotes = totals.map { (chain, total) ->
                    ChainQuote(chain = chain, estimatedTotal = total, savings = mostExpensive - total)
                }
            )
        }
}
