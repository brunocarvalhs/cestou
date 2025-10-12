package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class FinalizePurchaseUseCase @Inject constructor(
    private val repository: ShoppingCartRepository,
    private val createNewCartUseCase: CreateNewCartUseCase
) {
    suspend operator fun invoke(cartId: String?, market: String, price: Long): Result<Unit> =
        runCatching {
            if (cartId == null) return@runCatching
            val cart = repository.findById(cartId) ?: return@runCatching
            val finalizedCart = cart.finalizePurchase(market, price)
            repository.update(finalizedCart)
            createNewCartUseCase().getOrThrow()
        }
}
