package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class UpdateShoppingCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cart: ShoppingCart): Result<ShoppingCart> = runCatching {
        repository.update(cart)
            ?: throw ShoppingCartNotFoundException(cart.id)
    }
}
