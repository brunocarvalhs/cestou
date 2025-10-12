package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ProductNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository

class EditProductUseCase(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String, product: Product): Result<ShoppingCart> =
        runCatching {
            repository.updateProduct(cartId, product)
                ?: throw ProductNotFoundException(product.id)
        }
}
