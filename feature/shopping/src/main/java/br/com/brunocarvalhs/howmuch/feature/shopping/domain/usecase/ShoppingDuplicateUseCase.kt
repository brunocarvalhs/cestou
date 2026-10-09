package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.ShoppingTexts
import java.util.UUID
import javax.inject.Inject

class ShoppingDuplicateUseCase @Inject constructor(
    private val texts: ShoppingTexts,
    private val repository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase
) {
    suspend operator fun invoke(shopping: Shopping): Result<Shopping> = runCatching {
        val newId = UUID.randomUUID().toString()
        val copySuffix = texts.copySuffix()
        val duplicatedShopping = shopping.copy(
            id = newId,
            title = "${shopping.title} $copySuffix",
            status = Shopping.Status.NEW,
            createdAt = System.currentTimeMillis(),
            isFavorite = false
        )

        repository.create(duplicatedShopping)

        // Duplicar produtos
        productsUseCase(shopping.id).collect { products ->
            products.forEach { product ->
                productsUseCase.update(
                    product.copy(id = UUID.randomUUID().toString(), isPurchased = false),
                    newId
                )
            }
        }

        duplicatedShopping
    }
}
