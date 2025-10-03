package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository

class ShareShoppingCartUseCase(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String): Result<String> = runCatching {
        val cart = repository.findById(cartId)
            ?: throw ShoppingCartNotFoundException(cartId)
        cart.token
    }
}
