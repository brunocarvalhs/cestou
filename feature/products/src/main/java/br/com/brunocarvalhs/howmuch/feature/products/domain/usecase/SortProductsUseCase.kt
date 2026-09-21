package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import javax.inject.Inject

/**
 * UseCase responsável por ordenar a lista de produtos baseada no modo de ordenação.
 */
class SortProductsUseCase @Inject constructor() {
    operator fun invoke(products: List<Product>, sortingMode: String): List<Product> {
        return if (sortingMode == "NAME") {
            products.sortedBy { it.name.lowercase() }
        } else {
            products.sortedWith(
                compareBy<Product> { it.isPurchased }
                    .thenBy { it.category }
                    .thenBy { it.name }
            )
        }
    }
}
