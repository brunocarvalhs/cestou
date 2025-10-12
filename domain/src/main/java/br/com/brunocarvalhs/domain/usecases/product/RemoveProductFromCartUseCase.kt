package br.com.brunocarvalhs.domain.usecases.product

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ProductNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class RemoveProductFromCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String, productId: String): Result<ShoppingCart> =
        runCatching {
            repository.removeProduct(cartId, productId)
                ?: throw ProductNotFoundException(productId)
        }
}