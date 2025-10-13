package br.com.brunocarvalhs.domain.usecases.product

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.usecases.cart.UpdateShoppingCartUseCase
import javax.inject.Inject

class CheckProductUseCase @Inject constructor(
    private val cartLocalStorage: ICartLocalStorage,
    private val updateShoppingCartUseCase: UpdateShoppingCartUseCase
) {
    suspend operator fun invoke(
        product: Product,
        price: Long,
        isChecked: Boolean
    ): Result<Unit> = runCatching {
        val cart = cartLocalStorage.getCartNow()
            ?: throw Exception("Carrinho não encontrado.")

        val currentProducts = cart.products.toMutableList()
        val productIndex = currentProducts.indexOfFirst { it.id == product.id }

        if (productIndex == -1) {
            throw Exception("Produto não encontrado no carrinho.")
        }

        val updatedProduct = currentProducts[productIndex].toCopy(
            price = price,
            isChecked = isChecked
        )
        currentProducts[productIndex] = updatedProduct

        val updatedCart = cart.toCopy(products = currentProducts)
        updateShoppingCartUseCase(updatedCart).getOrThrow()
    }
}
