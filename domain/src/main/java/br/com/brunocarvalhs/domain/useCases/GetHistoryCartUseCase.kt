package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage

class GetHistoryCartUseCase(
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(): Result<List<ShoppingCart>> = runCatching {
        localStorage.getCartHistory()
    }
}
