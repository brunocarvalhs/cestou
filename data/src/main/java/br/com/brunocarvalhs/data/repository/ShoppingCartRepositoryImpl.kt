package br.com.brunocarvalhs.data.repository

import br.com.brunocarvalhs.data.model.ShoppingCartModel
import br.com.brunocarvalhs.data.model.toProductModel
import br.com.brunocarvalhs.data.model.toShoppingCartModel
import br.com.brunocarvalhs.data.services.RealtimeService
import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import javax.inject.Inject

class ShoppingCartRepositoryImpl @Inject constructor(
    private val service: RealtimeService
) : ShoppingCartRepository {

    override suspend fun create(cart: ShoppingCart?): ShoppingCart {
        val cartModel = cart as? ShoppingCartModel ?: cart?.let {
            ShoppingCartModel(
                id = cart.id,
                token = cart.token,
                items = cart.products.map { it.toProductModel() }.toMutableList(),
            )
        } ?: ShoppingCartModel()
        service.setValue(path = "${ShoppingCart.TABLE_NAME}/${cartModel.id}", value = cartModel)
        return cartModel
    }

    override suspend fun findById(id: String): ShoppingCartModel? =
        service.getValue(
            path = "${ShoppingCart.TABLE_NAME}/$id",
            clazz = ShoppingCartModel::class.java
        )

    override suspend fun findByToken(token: String): ShoppingCartModel? =
        service.queryByChild(
            path = ShoppingCart.TABLE_NAME,
            child = ShoppingCart.TOKEN,
            value = token,
            clazz = ShoppingCartModel::class.java
        )

    override suspend fun update(cart: ShoppingCart): ShoppingCartModel {
        val cartModel = cart.toShoppingCartModel()
        service.setValue(path = "${ShoppingCart.TABLE_NAME}/${cart.id}", value = cartModel)
        return cartModel
    }

    override suspend fun delete(id: String): Boolean {
        return try {
            service.removeValue(path = "${ShoppingCart.TABLE_NAME}/$id")
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun addProduct(cartId: String, product: Product): ShoppingCartModel? {
        val cart = findById(cartId) ?: return null
        cart.addProduct(product)
        update(cart)
        return cart
    }

    override suspend fun removeProduct(cartId: String, productId: String): ShoppingCartModel? {
        val cart = findById(cartId) ?: return null
        cart.removeProduct(productId)
        update(cart)
        return cart
    }

    override suspend fun updateProduct(cartId: String, product: Product): ShoppingCartModel? {
        val cart = findById(cartId) ?: return null
        cart.updateProduct(product)
        update(cart)
        return cart
    }

    override fun observeCart(cartId: String) =
        service.observe(
            path = "${ShoppingCart.TABLE_NAME}/$cartId",
            clazz = ShoppingCartModel::class.java
        )


}
