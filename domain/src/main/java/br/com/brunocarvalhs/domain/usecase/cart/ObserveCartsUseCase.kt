package br.com.brunocarvalhs.domain.usecase.cart

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class ObserveCartsUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    operator fun invoke() = repository.observeCarts()
}
