package br.com.brunocarvalhs.data.services

import br.com.brunocarvalhs.data.model.ShoppingCartModel
import br.com.brunocarvalhs.data.model.toShoppingCartModel
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.services.IDataStorageService
import javax.inject.Inject

class CartLocalStorage @Inject constructor(
    private val dataStorageService: IDataStorageService
) : ICartLocalStorage {

    override suspend fun saveCart(cart: ShoppingCart) {
        val history = getMutableCartHistory()
        history.add(cart.toShoppingCartModel())
        saveCartHistory(history)
        dataStorageService.removeValue(CART_LOCAL_STORAGE_KEY)
    }

    override suspend fun getCartHistory(): List<ShoppingCartModel> {
        return getMutableCartHistory().toList()
    }

    override suspend fun clearCartHistory() {
        dataStorageService.removeValue(CART_HISTORY_KEY)
    }

    override suspend fun getCartNow(): ShoppingCart? {
        return dataStorageService.getValue(CART_LOCAL_STORAGE_KEY, ShoppingCartModel::class.java)
    }

    override suspend fun saveCartNow(cart: ShoppingCart) {
        dataStorageService.saveValue(
            CART_LOCAL_STORAGE_KEY,
            cart.toShoppingCartModel(),
            ShoppingCartModel::class.java
        )
    }

    override suspend fun removeCartHistory(cart: ShoppingCart) {
        val history = getMutableCartHistory()
        val cartModel = cart.toShoppingCartModel()

        // Reutiliza a lógica centralizada de leitura e escrita.
        if (history.remove(cartModel)) {
            saveCartHistory(history)
        }
    }

    override suspend fun getCartLimit(): Long {
        val limit = dataStorageService.getValue(CARD_LIMIT_KEY, Long::class.java)
        return limit ?: MINIMAL_LIMIT
    }

    override suspend fun saveCartLimit(limit: Long) {
        dataStorageService.saveValue(CARD_LIMIT_KEY, limit, Long::class.java)
    }

    private suspend fun getMutableCartHistory(): MutableList<ShoppingCartModel> {
        return dataStorageService.getValue(CART_HISTORY_KEY, Array<ShoppingCartModel>::class.java)
            ?.toMutableList() ?: mutableListOf()
    }

    private suspend fun saveCartHistory(history: List<ShoppingCartModel>) {
        dataStorageService.saveValue(
            CART_HISTORY_KEY,
            history.toTypedArray(),
            Array<ShoppingCartModel>::class.java
        )
    }

    companion object {
        const val CART_HISTORY_KEY = "cart_history"
        const val CART_LOCAL_STORAGE_KEY = "shopping_cart"
        const val CARD_LIMIT_KEY = "card_limit"
        const val MINIMAL_LIMIT = 53000L
    }
}
