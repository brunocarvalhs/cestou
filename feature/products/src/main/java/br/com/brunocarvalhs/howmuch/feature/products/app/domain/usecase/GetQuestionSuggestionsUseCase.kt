package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class GetQuestionSuggestionsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(shoppingId: String): Flow<List<String>> = repository.getQuestionSuggestions(shoppingId)
}
