package br.com.brunocarvalhs.howmuch.feature.products.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductModelTest {

    @Test
    fun `toMap includes every field plus the computed total`() {
        val model = ProductModel(
            id = "1",
            name = "Arroz",
            quantity = 2.0,
            price = 5.0,
            isPurchased = true,
            category = "Mercearia",
            barcode = "789",
            unit = "kg"
        )

        val map = model.toMap()

        assertEquals("1", map["id"])
        assertEquals("Arroz", map["name"])
        assertEquals(2.0, map["quantity"])
        assertEquals(5.0, map["price"])
        assertEquals(true, map["isPurchased"])
        assertEquals("Mercearia", map["category"])
        assertEquals("789", map["barcode"])
        assertEquals("kg", map["unit"])
        assertEquals(10.0, map["total"])
        assertEquals(emptyList<Map<String, Any?>>(), map["history"])
    }

    @Test
    fun `toMap serializes history entries as maps`() {
        val model = ProductModel(
            id = "1",
            name = "Arroz",
            quantity = 2.0,
            history = listOf(ProductActivityModel(userId = "user-a", action = "ADDED", timestamp = 100L))
        )

        val history = model.toMap()["history"] as List<*>

        assertEquals(1, history.size)
        assertEquals(mapOf("userId" to "user-a", "action" to "ADDED", "timestamp" to 100L), history.first())
    }

    @Test
    fun `toMap computes a zero total when price is null`() {
        val model = ProductModel(id = "1", name = "Arroz", quantity = 2.0, price = null)

        assertEquals(0.0, model.toMap()["total"])
    }

    @Test
    fun `fromMap parses a complete map`() {
        val map = mapOf(
            "id" to "1",
            "name" to "Arroz",
            "quantity" to 2,
            "price" to 5,
            "isPurchased" to true,
            "category" to "Mercearia",
            "barcode" to "789",
            "unit" to "kg"
        )

        val model = ProductModel.fromMap(map)

        assertEquals("1", model.id)
        assertEquals("Arroz", model.name)
        assertEquals(2.0, model.quantity, 0.0)
        assertEquals(5.0, model.price)
        assertEquals(true, model.isPurchased)
        assertEquals("Mercearia", model.category)
        assertEquals("789", model.barcode)
        assertEquals("kg", model.unit)
    }

    @Test
    fun `fromMap falls back to defaults for missing optional fields`() {
        val map = mapOf("id" to "1", "name" to "Arroz")

        val model = ProductModel.fromMap(map)

        assertEquals(1.0, model.quantity, 0.0)
        assertEquals(0.0, model.price)
        assertEquals(false, model.isPurchased)
        assertEquals("Outros", model.category)
        assertEquals(null, model.barcode)
        assertEquals("un", model.unit)
        assertEquals(emptyList<ProductActivityModel>(), model.history)
    }

    @Test
    fun `fromMap parses history entries back into ProductActivityModel`() {
        val map = mapOf(
            "id" to "1",
            "name" to "Arroz",
            "history" to listOf(mapOf("userId" to "user-a", "action" to "ADDED", "timestamp" to 100L))
        )

        val model = ProductModel.fromMap(map)

        assertEquals(1, model.history.size)
        assertEquals("user-a", model.history.first().userId)
        assertEquals("ADDED", model.history.first().action)
        assertEquals(100L, model.history.first().timestamp)
    }

    @Test
    fun `fromMap ignores a missing history key without crashing`() {
        val model = ProductModel.fromMap(mapOf("id" to "1", "name" to "Arroz"))

        assertEquals(emptyList<ProductActivityModel>(), model.history)
    }
}
