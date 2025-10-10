package br.com.brunocarvalhs.data.model

import br.com.brunocarvalhs.domain.entities.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductModelTest {

    @Test
    fun `default values should be set correctly`() {
        // Act
        val product = ProductModel()

        // Assert
        assertEquals(36, product.id.length) // UUID length
        assertEquals("", product.name)
        assertEquals(null, product.price)
        assertEquals(1, product.quantity)
    }

    @Test
    fun `toCopy should create a copy with updated values`() {
        // Arrange
        val product = ProductModel()
        val newId = "new-id"
        val newName = "New Product"
        val newPrice = 100L
        val newQuantity = 5

        // Act
        val copiedProduct = product.toCopy(
            id = newId,
            name = newName,
            price = newPrice,
            quantity = newQuantity
        )

        // Assert
        assertEquals(newId, copiedProduct.id)
        assertEquals(newName, copiedProduct.name)
        assertEquals(newPrice, copiedProduct.price)
        assertEquals(newQuantity, copiedProduct.quantity)
    }

    @Test
    fun `toProductModel should convert Product to ProductModel`() {
        // Arrange
        val product: Product = ProductModel(
            id = "test-id",
            name = "Test Product",
            price = 50L,
            quantity = 2
        )

        // Act
        val productModel = product.toProductModel()

        // Assert
        assertEquals("test-id", productModel.id)
        assertEquals("Test Product", productModel.name)
        assertEquals(50L, productModel.price)
        assertEquals(2, productModel.quantity)
    }
}
