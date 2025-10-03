package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository

class CreateShoppingCartUseCase(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cart: ShoppingCart): Result<ShoppingCart> = runCatching {
        repository.create(cart)
    }
}
