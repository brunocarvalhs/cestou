package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveShoppingCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository,
) {
    operator fun invoke(cartId: String): Flow<ShoppingCart?> {
        return repository.observeCarts().map { carts ->
            carts.find { it.id == cartId }
        }
    }
}
