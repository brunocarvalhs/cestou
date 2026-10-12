package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProductsUseCaseImpl @Inject constructor(
    private val repository: ProductRepository
) : ProductsUseCase {
    override suspend operator fun invoke(shoppingId: String): Flow<List<Product>> =
        repository.getAllProducts(shoppingId)

    override suspend fun delete(productId: String, shoppingId: String): Result<Unit> =
        repository.deleteProduct(productId, shoppingId)

    override suspend fun update(product: Product, shoppingId: String): Result<Unit> =
        repository.updateProduct(product, shoppingId)

    override suspend fun save(product: Product, shoppingId: String): Result<Unit> =
        repository.saveProduct(product, shoppingId)

    override suspend fun move(product: Product, currentShoppingId: String, targetShoppingId: String): Result<Unit> {
        return runCatching {
            delete(product.id, currentShoppingId).getOrThrow()
            save(product, targetShoppingId).getOrThrow()
        }
    }
}
