package br.com.brunocarvalhs.domain.usecases.history

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class CreateShoppingListFromHistoryUseCase @Inject constructor(
    private val repository: ShoppingCartRepository,
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(cart: ShoppingCart): Result<Unit> = runCatching {
        val newCart = cart.cloneCart()
        localStorage.saveCartNow(newCart)
        repository.create(newCart)
    }
}
