package br.com.brunocarvalhs.howmuch.feature.products.data.extensions

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.feature.products.data.model.ProductActivityModel
import br.com.brunocarvalhs.howmuch.feature.products.data.model.ProductModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMapperTest {

    private val domain = Product(
        id = "1",
        name = "Test Product",
        quantity = 2.0,
        price = 10.0,
        isPurchased = true,
        category = "Electronics",
        barcode = "123456",
        history = listOf(ProductActivity(userId = "user-a", action = ProductActivity.Action.ADDED, timestamp = 100L)),
    )

    private val model = ProductModel(
        id = "1",
        name = "Test Product",
        quantity = 2.0,
        price = 10.0,
        isPurchased = true,
        category = "Electronics",
        barcode = "123456",
        history = listOf(ProductActivityModel(userId = "user-a", action = "ADDED", timestamp = 100L)),
    )

    @Test
    fun `toDomain should map model to domain entity`() {
        val result = model.toDomain()

        assertEquals(domain.id, result.id)
        assertEquals(domain.name, result.name)
        assertEquals(domain.quantity, result.quantity, 0.0)
        assertEquals(domain.price, result.price)
        assertEquals(domain.isPurchased, result.isPurchased)
        assertEquals(domain.category, result.category)
        assertEquals(domain.barcode, result.barcode)
        assertEquals(domain.history, result.history)
    }

    @Test
    fun `toModel should map domain entity to model`() {
        val result = domain.toModel()

        assertEquals(model.id, result.id)
        assertEquals(model.name, result.name)
        assertEquals(model.quantity, result.quantity, 0.0)
        assertEquals(model.price, result.price)
        assertEquals(model.isPurchased, result.isPurchased)
        assertEquals(model.category, result.category)
        assertEquals(model.barcode, result.barcode)
        assertEquals(model.history, result.history)
    }

    @Test
    fun `toDomain maps an empty history to an empty list`() {
        val result = model.copy(history = emptyList()).toDomain()

        assertEquals(emptyList<ProductActivity>(), result.history)
    }
}
