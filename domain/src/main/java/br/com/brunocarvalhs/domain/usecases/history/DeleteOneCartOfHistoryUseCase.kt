package br.com.brunocarvalhs.domain.usecases.history

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class DeleteOneCartOfHistoryUseCase @Inject constructor(
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(cart: ShoppingCart): Result<Unit> = runCatching {
        localStorage.removeCartHistory(cart)
    }
}