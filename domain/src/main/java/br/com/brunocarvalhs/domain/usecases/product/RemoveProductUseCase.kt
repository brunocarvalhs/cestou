package br.com.brunocarvalhs.domain.usecases.product

import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.usecases.cart.UpdateShoppingCartUseCase
import javax.inject.Inject

/**
 * Use case responsible for removing a product from the current shopping cart.
 */
class RemoveProductUseCase @Inject constructor(
    private val cartLocalStorage: ICartLocalStorage,
    private val updateShoppingCartUseCase: UpdateShoppingCartUseCase
) {
    /**
     * Removes a product specified by its ID from the shopping cart.
     *
     * @param productId The ID of the product to be removed.
     * @return A [Result] indicating the success or failure of the operation.
     */
    suspend operator fun invoke(productId: String): Result<Unit> = runCatching {
        val cart = cartLocalStorage.getCartNow()
            ?: throw Exception("Carrinho não encontrado.")

        val currentProducts = cart.products.toMutableList()
        val productIndex = currentProducts.indexOfFirst { it.id == productId }

        if (productIndex == -1) {
            throw Exception("Produto não encontrado no carrinho para remoção.")
        }

        currentProducts.removeAt(productIndex)

        val updatedCart: ShoppingCart = cart.toCopy(products = currentProducts)

        updateShoppingCartUseCase(updatedCart).getOrThrow()
    }
}
