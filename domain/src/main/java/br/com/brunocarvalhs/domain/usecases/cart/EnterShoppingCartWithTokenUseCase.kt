package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.InvalidTokenException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class EnterShoppingCartWithTokenUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(token: String): Result<ShoppingCart> = runCatching {
        repository.findByToken(token)
            ?: throw InvalidTokenException(token)
    }
}