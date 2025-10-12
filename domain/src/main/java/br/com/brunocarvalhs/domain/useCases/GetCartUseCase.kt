package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage

class GetCartUseCase(
    private val repository: ShoppingCartRepository,
    private val cartLocalStorage: ICartLocalStorage,
) {
    suspend operator fun invoke(cartId: String): Result<ShoppingCart> = runCatching {
        repository.findById(cartId) ?: cartLocalStorage.getCartHistory().find { it.id == cartId }
        ?: error("Cart not found")
    }
}