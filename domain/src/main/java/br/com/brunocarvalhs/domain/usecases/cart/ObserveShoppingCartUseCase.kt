package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class ObserveShoppingCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository,
) {
    operator fun invoke(cartId: String) = repository.observeCart(cartId)
}