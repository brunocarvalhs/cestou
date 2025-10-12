package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository

class ObserveShoppingCartUseCase(
    private val repository: ShoppingCartRepository,
) {
    operator fun invoke(cartId: String) = repository.observeCart(cartId)
}
