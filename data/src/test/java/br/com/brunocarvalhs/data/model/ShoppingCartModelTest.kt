package br.com.brunocarvalhs.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingCartModelTest {

    @Test
    fun `default values should be set correctly`() {
        // Act
        val cart = ShoppingCartModel()

        // Assert
        assertEquals(36, cart.id.length) // UUID length
        assertEquals(6, cart.token.length) // Token length
        assertTrue(cart.products.isEmpty())
        assertEquals("", cart.market)
        assertEquals(0L, cart.totalPrice)
        assertEquals(null, cart.purchaseDate)
    }

    @Test
    fun `addProduct should add a new product`() {
        // Arrange
        val cart = ShoppingCartModel()
        val product = ProductModel(id = "product1", name = "Product 1", price = 100L, quantity = 1)

        // Act
        cart.addProduct(product)

        // Assert
        assertEquals(1, cart.products.size)
        assertEquals(product, cart.products[0])
    }

    @Test
    fun `removeProduct should remove the product by id`() {
        // Arrange
        val product = ProductModel(id = "product1", name = "Product 1", price = 100L, quantity = 1)
        val cart = ShoppingCartModel(items = mutableListOf(product))

        // Act
        cart.removeProduct("product1")

        // Assert
        assertTrue(cart.products.isEmpty())
    }

    @Test
    fun `recalculateTotal should return the correct total price`() {
        // Arrange
        val product1 = ProductModel(id = "product1", name = "Product 1", price = 100L, quantity = 2)
        val product2 = ProductModel(id = "product2", name = "Product 2", price = 50L, quantity = 3)
        val cart = ShoppingCartModel(items = mutableListOf(product1, product2))

        // Act
        val total = cart.recalculateTotal()

        // Assert
        assertEquals(350L, total)
    }

    @Test
    fun `finalizePurchase should update name, market, and purchaseDate`() {
        // Arrange
        val cart = ShoppingCartModel()
        val market = "Test Market"
        val price = 500L

        // Act
        val finalizedCart = cart.finalizePurchase( market, price)

        // Assert
        assertEquals(market, finalizedCart.market)
        assertEquals(price, finalizedCart.totalPrice)
        assertTrue(finalizedCart.purchaseDate != null)
    }
}
