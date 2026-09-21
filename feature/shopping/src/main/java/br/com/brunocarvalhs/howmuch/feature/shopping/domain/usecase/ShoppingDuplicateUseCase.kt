package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

class ShoppingDuplicateUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase
) {
    suspend operator fun invoke(shopping: Shopping): Result<Shopping> = runCatching {
        val newId = UUID.randomUUID().toString()
        val copySuffix = context.getString(R.string.shopping_list_copy_suffix)
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
