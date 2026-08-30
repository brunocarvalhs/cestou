package br.com.brunocarvalhs.howmuch.feature.shopping.data.mapper

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.feature.shopping.data.model.ShoppingModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingMapperTest {

    companion object {
        private const val PRICE = 100.0
        private const val CREATED_AT = 123456789L
        private const val BUDGET = 500.0
    }

    private val domain = Shopping(
        id = "1",
        title = "Test Shopping",
        description = "Description",
        price = PRICE,
        status = Shopping.Status.IN_PROGRESS,
        users = listOf("user1"),
        roles = mapOf("user1" to "admin"),
        createdAt = CREATED_AT,
        isFavorite = true,
        isCategorized = false,
        shortCode = "ABC",
        budget = BUDGET,
        position = 1
    )

    private val model = ShoppingModel(
        id = "1",
        title = "Test Shopping",
        description = "Description",
        price = PRICE,
        status = Shopping.Status.IN_PROGRESS,
        users = listOf("user1"),
        roles = mapOf("user1" to "admin"),
        createdAt = CREATED_AT,
        isFavorite = true,
        isCategorized = false,
        shortCode = "ABC",
        budget = BUDGET,
        position = 1
    )

    @Test
    fun `toDomain should map model to domain entity`() {
        val result = model.toDomain()
        
        assertEquals(domain.id, result.id)
        assertEquals(domain.title, result.title)
        assertEquals(domain.description, result.description)
        assertEquals(domain.price, result.price, 0.0)
        assertEquals(domain.status, result.status)
        assertEquals(domain.users, result.users)
        assertEquals(domain.roles, result.roles)
        assertEquals(domain.createdAt, result.createdAt)
        assertEquals(domain.isFavorite, result.isFavorite)
        assertEquals(domain.isCategorized, result.isCategorized)
        assertEquals(domain.shortCode, result.shortCode)
        assertEquals(domain.budget, result.budget)
        assertEquals(domain.position, result.position)
    }

    @Test
    fun `toModel should map domain entity to model`() {
        val result = domain.toModel()
        
        assertEquals(model.id, result.id)
        assertEquals(model.title, result.title)
        assertEquals(model.description, result.description)
        assertEquals(model.price, result.price, 0.0)
        assertEquals(model.status, result.status)
        assertEquals(model.users, result.users)
        assertEquals(model.roles, result.roles)
        assertEquals(model.createdAt, result.createdAt)
        assertEquals(model.isFavorite, result.isFavorite)
        assertEquals(model.isCategorized, result.isCategorized)
        assertEquals(model.shortCode, result.shortCode)
        assertEquals(model.budget, result.budget)
        assertEquals(model.position, result.position)
    }
}
