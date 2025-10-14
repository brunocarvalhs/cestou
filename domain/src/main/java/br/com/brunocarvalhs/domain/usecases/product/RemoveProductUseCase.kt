package br.com.brunocarvalhs.domain.usecases.product

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.exceptions.ShoppingCartNotFoundException
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.usecases.cart.UpdateShoppingCartUseCase
import javax.inject.Inject

/**
 * Use case responsible for removing a product from the current shopping cart.
 */
class RemoveProductUseCase @Inject constructor(
    private val repository: ShoppingCartRepository,
    private val cartLocalStorage: ICartLocalStorage,
) {
    /**
     * Removes a product specified by its ID from the shopping cart.
     *
     * @param productId The ID of the product to be removed.
     * @return A [Result] indicating the success or failure of the operation.
     */
    suspend operator fun invoke(product: Product): Result<Unit> = runCatching {
        val cart = cartLocalStorage.getCartNow()?.id?.let { id ->
            repository.findById(id)
        } ?: throw ShoppingCartNotFoundException()

        val currentProducts = cart.products.toMutableList()

        currentProducts.remove(product)

        val updatedCart: ShoppingCart = cart.toCopy(products = currentProducts)

        repository.update(updatedCart)
    }
}
