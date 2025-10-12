package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class FinalizePurchaseUseCase @Inject constructor(
    private val shoppingCartRepository: ShoppingCartRepository,
    private val localStorage: ICartLocalStorage
) {
    suspend operator fun invoke(id: String, market: String, price: Long): Result<Unit> =
        runCatching {
            val cart = shoppingCartRepository.findById(id)
                ?: error("Shopping cart not found")
            val finalizedCart = cart.finalizePurchase(market = market, price = price)
            localStorage.saveCart(finalizedCart)
            shoppingCartRepository.delete(cart.id)
        }
}