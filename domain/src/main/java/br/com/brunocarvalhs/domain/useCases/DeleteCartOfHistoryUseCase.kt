package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage

class DeleteCartOfHistoryUseCase(
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(list: List<ShoppingCart>): Result<Unit> = runCatching {
        list.forEach { localStorage.removeCartHistory(it) }
    }
}
