package br.com.brunocarvalhs.domain.usecases.history

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class GetHistoryCartUseCase @Inject constructor(
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(): Result<List<ShoppingCart>> = runCatching {
        localStorage.getCartHistory()
    }
}
