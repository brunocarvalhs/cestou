package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class ShareShoppingCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String): Result<String> = runCatching {
        val cart = repository.findById(cartId)
            ?: throw ShoppingCartNotFoundException(cartId)
        cart.token
    }
}