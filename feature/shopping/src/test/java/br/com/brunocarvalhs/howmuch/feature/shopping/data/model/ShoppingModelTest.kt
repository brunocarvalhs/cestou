package br.com.brunocarvalhs.howmuch.feature.shopping.data.model

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.typeOf

/**
 * Reproduces, at the unit-test level, the exact reflective serializer lookup that
 * `NetworkManager.getResponse()` performs at runtime for every Firestore response
 * (`json.serializersModule.serializer(responseType)`, mirrored below via `typeOf<List<ShoppingModel>>()`).
 *
 * `ShoppingModel` once shipped without `@Serializable`. That did not fail the build: kotlinx.serialization
 * only generates a serializer for annotated classes, so the reflective lookup below threw
 * `SerializationException: Serializer for class 'ShoppingModel' is not found` at runtime. `NetworkManager`
 * catches that exception and falls back to `null`, so every shopping-list read silently returned an empty
 * list even though the writes to Firestore succeeded. See
 * `.claude/skills/firestore-serializable-models/SKILL.md`.
 */
class ShoppingModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val model = ShoppingModel(
        id = "1",
        title = "Compras da semana",
        description = "Mercado",
        price = 100.0,
        status = Shopping.Status.NEW,
        users = listOf("user-1"),
        roles = mapOf("user-1" to "OWNER"),
        createdAt = 123456789L,
        isFavorite = true,
        isCategorized = false,
        shortCode = "ABC123",
        budget = 500.0,
        position = 1
    )

    @Test
    fun `NetworkManager's reflective serializer lookup resolves and decodes ShoppingModel`() {
        val serializer = json.serializersModule.serializer(typeOf<List<ShoppingModel>>())
        val encoded = json.encodeToString(serializer, listOf(model))

        @Suppress("UNCHECKED_CAST")
        val decoded = json.decodeFromString(serializer, encoded) as List<ShoppingModel>

        assertEquals(listOf(model), decoded)
    }

    @Test
    fun `ShoppingModel round-trips through kotlinx serialization unchanged`() {
        val encoded = json.encodeToString(ShoppingModel.serializer(), model)
        val decoded = json.decodeFromString(ShoppingModel.serializer(), encoded)

        assertEquals(model, decoded)
    }
}
