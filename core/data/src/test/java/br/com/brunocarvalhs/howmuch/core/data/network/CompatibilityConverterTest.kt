package br.com.brunocarvalhs.howmuch.core.data.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityConverterTest {

    private val converter = CompatibilityConverter()

    @Test
    fun `toJsonElement converts primitives`() {
        assertEquals(JsonNull, converter.toJsonElement(null))
        assertEquals(JsonPrimitive(true), converter.toJsonElement(true))
        assertEquals(JsonPrimitive(42), converter.toJsonElement(42))
        assertEquals(JsonPrimitive("text"), converter.toJsonElement("text"))
    }

    @Test
    fun `toJsonElement converts an iterable to a JsonArray`() {
        val result = converter.toJsonElement(listOf("a", "b"))

        assertEquals(JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b"))), result)
    }

    @Test
    fun `toJsonElement converts a map with non-numeric keys to a JsonObject`() {
        val result = converter.toJsonElement(mapOf("title" to "Weekly Groceries"))

        assertEquals(JsonObject(mapOf("title" to JsonPrimitive("Weekly Groceries"))), result)
    }

    @Test
    fun `toJsonElement converts a numerically-indexed map to a JsonArray ordered by key`() {
        val result = converter.toJsonElement(mapOf("1" to "b", "0" to "a"))

        assertEquals(JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b"))), result)
    }

    @Test
    fun `toJsonElement falls back to toString for unknown types`() {
        val result = converter.toJsonElement(StringBuilder("custom"))

        assertEquals(JsonPrimitive("custom"), result)
    }

    @Test
    fun `listToTypedArray copies matching items into a typed array`() {
        val array = converter.listToTypedArray(listOf("a", "b"), Array<String>::class.java)

        assertTrue(array is Array<*>)
        assertEquals(listOf("a", "b"), (array as Array<*>).toList())
    }

    @Test
    fun `listToTypedArray returns null when the array class has no component type`() {
        val result = converter.listToTypedArray(listOf("a"), String::class.java)

        assertEquals(null, result)
    }
}
