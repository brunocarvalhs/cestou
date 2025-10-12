package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class CreateShoppingCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cart: ShoppingCart): Result<ShoppingCart> = runCatching {
        repository.create(cart)
        repository.findById(cart.id) ?: throw Exception("Shopping cart not found")
    }
}