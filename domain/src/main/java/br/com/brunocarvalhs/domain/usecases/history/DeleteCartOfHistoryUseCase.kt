package br.com.brunocarvalhs.domain.usecases.history

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class DeleteCartOfHistoryUseCase @Inject constructor(
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(list: List<ShoppingCart>): Result<Unit> = runCatching {
        list.forEach { localStorage.removeCartHistory(it) }
    }
}
