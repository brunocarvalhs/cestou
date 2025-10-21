package br.com.brunocarvalhs.domain.services

import br.com.brunocarvalhs.domain.entities.ShoppingCart

interface ICartLocalStorage {
    suspend fun saveCart(cart: ShoppingCart)
    suspend fun getCartHistory(): List<ShoppingCart>
    suspend fun clearCartHistory()
    suspend fun getCartNow(): ShoppingCart?

    suspend fun saveCartNow(cart: ShoppingCart)
    suspend fun removeCartHistory(cart: ShoppingCart)

    suspend fun getCartLimit(): Long

    suspend fun saveCartLimit(limit: Long)

    suspend fun getCartById(id: String): ShoppingCart?
}
