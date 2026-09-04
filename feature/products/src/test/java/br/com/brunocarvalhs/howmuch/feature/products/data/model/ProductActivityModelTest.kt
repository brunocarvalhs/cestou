package br.com.brunocarvalhs.howmuch.feature.products.data.model

import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductActivityModelTest {

    @Test
    fun `fromMap parses a valid entry`() {
        val map = mapOf("userId" to "user-a", "action" to "ADDED", "timestamp" to 100L)

        val model = ProductActivityModel.fromMap(map)

        assertEquals(ProductActivityModel(userId = "user-a", action = "ADDED", timestamp = 100L), model)
    }

    @Test
    fun `fromMap returns null when userId is missing`() {
        assertNull(ProductActivityModel.fromMap(mapOf("action" to "ADDED", "timestamp" to 100L)))
    }

    @Test
    fun `fromMap returns null when action is missing`() {
        assertNull(ProductActivityModel.fromMap(mapOf("userId" to "user-a", "timestamp" to 100L)))
    }

    @Test
    fun `fromMap defaults a missing timestamp to zero instead of failing`() {
        val model = ProductActivityModel.fromMap(mapOf("userId" to "user-a", "action" to "ADDED"))

        assertEquals(0L, model?.timestamp)
    }

    @Test
    fun `toDomain converts a valid action`() {
        val model = ProductActivityModel(userId = "user-a", action = "EDITED", timestamp = 100L)

        assertEquals(
            ProductActivity(userId = "user-a", action = ProductActivity.Action.EDITED, timestamp = 100L),
            model.toDomain()
        )
    }

    @Test
    fun `toDomain returns null for an unrecognized action instead of throwing`() {
        val model = ProductActivityModel(userId = "user-a", action = "NOT_A_REAL_ACTION", timestamp = 100L)

        assertNull(model.toDomain())
    }
}
