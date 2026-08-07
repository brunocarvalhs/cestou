package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import java.util.UUID
import javax.inject.Inject

internal class ShoppingDuplicateUseCase @Inject constructor(
    private val repository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase
) {
    suspend operator fun invoke(shopping: Shopping): Result<Shopping> = runCatching {
        val newId = UUID.randomUUID().toString()
        val duplicatedShopping = shopping.copy(
            id = newId,
            title = "${shopping.title} (Cópia)",
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
