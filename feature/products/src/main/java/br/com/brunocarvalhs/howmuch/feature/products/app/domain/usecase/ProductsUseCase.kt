package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(shoppingId: String): Flow<List<Product>> = repository.getAllProducts(shoppingId)

    suspend fun delete(productId: String, shoppingId: String): Result<Unit> =
        repository.deleteProduct(productId, shoppingId)

    suspend fun update(product: Product, shoppingId: String): Result<Unit> =
        repository.updateProduct(product, shoppingId)

    suspend fun save(product: Product, shoppingId: String): Result<Unit> =
        repository.saveProduct(product, shoppingId)

    suspend fun move(product: Product, currentShoppingId: String, targetShoppingId: String): Result<Unit> {
        return runCatching {
            delete(product.id, currentShoppingId).getOrThrow()
            save(product, targetShoppingId).getOrThrow()
        }
    }
}
