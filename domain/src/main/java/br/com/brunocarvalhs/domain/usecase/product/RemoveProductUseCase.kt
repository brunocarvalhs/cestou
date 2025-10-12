package br.com.brunocarvalhs.domain.usecase.product

import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class RemoveProductUseCase @Inject constructor(
    private val repository: ShoppingCartRepository
) {
    suspend operator fun invoke(cartId: String, productId: String) = repository.removeProduct(cartId, productId)
}
