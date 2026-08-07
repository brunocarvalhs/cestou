package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class GetProductSuggestionsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(shoppingId: String): Flow<List<Product>> = repository.getSuggestions(shoppingId)
}
