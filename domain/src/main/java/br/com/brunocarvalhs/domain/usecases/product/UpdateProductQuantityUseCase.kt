package br.com.brunocarvalhs.domain.usecases.product

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.usecases.cart.UpdateShoppingCartUseCase
import javax.inject.Inject

/**
 * Use case responsible for updating the quantity of a specific product in the shopping cart.
 */
class UpdateProductQuantityUseCase @Inject constructor(
    private val repository: ShoppingCartRepository,
    private val cartLocalStorage: ICartLocalStorage,
) {
    /**
     * Updates the quantity of a product.
     *
     * @param productId The ID of the product to update.
     * @param newQuantity The new quantity for the product. Must be 1 or greater.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(
        product: Product,
        newQuantity: Int
    ): Result<Unit> = runCatching {
        if (newQuantity < 1) {
            throw IllegalArgumentException("A quantidade deve ser de pelo menos 1.")
        }

        val cart = cartLocalStorage.getCartNow()?.id?.let { id ->
            repository.findById(id)
        } ?: throw Exception("Carrinho não encontrado.")

        val currentProducts = cart.products.toMutableList()
        val productIndex = currentProducts.indexOfFirst { it.id == product.id }

        if (productIndex == -1) {
            throw Exception("Produto não encontrado no carrinho.")
        }

        val updatedProduct = currentProducts[productIndex].toCopy(quantity = newQuantity)
        currentProducts[productIndex] = updatedProduct

        val updatedCart = cart.toCopy(products = currentProducts)

        repository.update(updatedCart)
    }
}
