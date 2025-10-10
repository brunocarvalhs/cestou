package br.com.brunocarvalhs.domain.useCases

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.InvalidTokenException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository

class EnterShoppingCartWithTokenUseCase(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(token: String): Result<ShoppingCart> = runCatching {
        repository.findByToken(token)
            ?: throw InvalidTokenException(token)
    }
}
