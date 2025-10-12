package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class CreateNewCartUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        repository.createNewCart()
    }
}
