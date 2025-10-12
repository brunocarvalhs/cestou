package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class DeleteShoppingCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String): Result<Unit> = runCatching {
        val success = repository.delete(cartId)
        if (success) Result.success(Unit)
        else throw ShoppingCartNotFoundException(cartId)
    }
}