package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class PublishCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cart: ShoppingCart) = repository.publishCart(cart)
}
