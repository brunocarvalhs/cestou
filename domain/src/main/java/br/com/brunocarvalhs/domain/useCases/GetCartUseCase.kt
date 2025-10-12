package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository

class GetCartUseCase(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String): Result<ShoppingCart> = runCatching {
        repository.findById(cartId) ?: error("Cart not found")
    }
}