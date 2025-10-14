package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class ObserveShoppingCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository,
    private val cartLocalStorage: ICartLocalStorage
) {
    suspend operator fun invoke() = runCatching {
        val cart = cartLocalStorage.getCartNow() ?: run {
            val newCart = repository.create()
            cartLocalStorage.saveCartNow(newCart)
            return@run newCart
        }
        repository.observeCart(cart.id)
    }
}
