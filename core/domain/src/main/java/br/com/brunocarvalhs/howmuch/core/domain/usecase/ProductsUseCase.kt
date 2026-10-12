package br.com.brunocarvalhs.howmuch.core.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * AD-011: contract for product CRUD consumed by `feature/cart` and `feature/shopping`.
 * Implemented by `ProductsUseCaseImpl` in `feature/products`, bound via Hilt.
 */
interface ProductsUseCase {
    suspend operator fun invoke(shoppingId: String): Flow<List<Product>>
    suspend fun delete(productId: String, shoppingId: String): Result<Unit>
    suspend fun update(product: Product, shoppingId: String): Result<Unit>
    suspend fun save(product: Product, shoppingId: String): Result<Unit>
    suspend fun move(product: Product, currentShoppingId: String, targetShoppingId: String): Result<Unit>
}
