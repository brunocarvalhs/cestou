package br.com.brunocarvalhs.domain.usecases.history

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class GetCartByIdUseCase @Inject constructor(
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(id: String): Result<ShoppingCart?> = runCatching {
        localStorage.getCartById(id)
    }
}
