package br.com.brunocarvalhs.howmuch.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductTest {

    private val base = Product(id = "1", name = "Arroz", quantity = 2.0, price = 5.0)

    @Test
    fun `addedBy returns null when history is empty`() {
        assertNull(base.addedBy)
    }

    @Test
    fun `addedBy returns the first ADDED entry's userId`() {
        val product = base.withActivity(ProductActivity.Action.ADDED, "user-a")
            .withActivity(ProductActivity.Action.EDITED, "user-b")

        assertEquals("user-a", product.addedBy)
    }

    @Test
    fun `lastEditedBy and purchasedBy return the last matching entry, not the first`() {
        val product = base
            .withActivity(ProductActivity.Action.ADDED, "user-a")
            .withActivity(ProductActivity.Action.EDITED, "user-a")
            .withActivity(ProductActivity.Action.EDITED, "user-b")
            .withActivity(ProductActivity.Action.PURCHASED, "user-b")

        assertEquals("user-b", product.lastEditedBy)
        assertEquals("user-b", product.purchasedBy)
    }

    @Test
    fun `withActivity appends without mutating the original instance`() {
        val updated = base.withActivity(ProductActivity.Action.ADDED, "user-a")

        assertEquals(0, base.history.size)
        assertEquals(1, updated.history.size)
        assertEquals("user-a", updated.history.first().userId)
        assertEquals(ProductActivity.Action.ADDED, updated.history.first().action)
    }

    @Test
    fun `lastActivity returns the most recent entry by timestamp`() {
        val product = base.copy(
            history = listOf(
                ProductActivity(userId = "user-a", action = ProductActivity.Action.ADDED, timestamp = 100L),
                ProductActivity(userId = "user-b", action = ProductActivity.Action.EDITED, timestamp = 200L),
            )
        )

        assertEquals("user-b", product.lastActivity?.userId)
    }
}
