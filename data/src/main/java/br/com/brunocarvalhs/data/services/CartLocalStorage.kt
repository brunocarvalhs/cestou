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
        val existingHistory: MutableList<ShoppingCartModel> =
            dataStorageService.getValue(CART_HISTORY_KEY, Array<ShoppingCartModel>::class.java)
                ?.toMutableList() ?: mutableListOf()

        val cartModel = cart.toShoppingCartModel()

        existingHistory.add(cartModel)
        dataStorageService.saveValue(CART_HISTORY_KEY, existingHistory, List::class.java)
        dataStorageService.removeValue(CART_LOCAL_STORAGE_KEY)
    }

    override suspend fun getCartHistory(): List<ShoppingCartModel> {
        return dataStorageService.getValue(CART_HISTORY_KEY, Array<ShoppingCartModel>::class.java)
            ?.toList() ?: emptyList()
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
            cart as ShoppingCartModel,
            ShoppingCartModel::class.java
        )
    }

    override suspend fun removeCartHistory(cart: ShoppingCart) {
        val existingHistory: MutableList<ShoppingCartModel> =
            dataStorageService.getValue(CART_HISTORY_KEY, Array<ShoppingCartModel>::class.java)
                ?.toMutableList() ?: mutableListOf()

        val cartModel = cart.toShoppingCartModel()

        existingHistory.remove(cartModel)
        dataStorageService.saveValue(CART_HISTORY_KEY, existingHistory, List::class.java)
    }

    companion object {
        const val CART_HISTORY_KEY = "cart_history"

        const val CART_LOCAL_STORAGE_KEY = "shopping_cart"
    }
}
