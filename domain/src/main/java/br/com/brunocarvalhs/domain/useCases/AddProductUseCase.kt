package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository

class AddProductUseCase(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String, product: Product): Result<ShoppingCart> =
        runCatching {
            repository.addProduct(cartId, product)
                ?: throw ShoppingCartNotFoundException(cartId)
        }
}
