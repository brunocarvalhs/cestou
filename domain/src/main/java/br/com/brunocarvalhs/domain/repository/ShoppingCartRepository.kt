package br.com.brunocarvalhs.domain.repository

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import kotlinx.coroutines.flow.Flow

interface ShoppingCartRepository {

    /**
     * Cria um novo carrinho de compras.
     */
    suspend fun create(cart: ShoppingCart? = null): ShoppingCart

    /**
     * Busca um carrinho pelo ID.
     */
    suspend fun findById(id: String): ShoppingCart?

    /**
     * Busca um carrinho pelo token (para compartilhamento).
     */
    suspend fun findByToken(token: String): ShoppingCart?

    /**
     * Atualiza os dados de um carrinho (ex: adicionar produtos).
     */
    suspend fun update(cart: ShoppingCart): ShoppingCart?

    /**
     * Remove um carrinho pelo ID.
     */
    suspend fun delete(id: String): Boolean

    /**
     * Adiciona um produto a um carrinho.
     */
    suspend fun addProduct(cartId: String, product: Product): ShoppingCart?

    /**
     * Remove um produto de um carrinho.
     */
    suspend fun removeProduct(cartId: String, productId: String): ShoppingCart?

    /**
     * Atualiza a quantidade/preço de um produto em um carrinho.
     */
    suspend fun updateProduct(cartId: String, product: Product): ShoppingCart?

    fun observeCart(cartId: String): Flow<ShoppingCart>
}
