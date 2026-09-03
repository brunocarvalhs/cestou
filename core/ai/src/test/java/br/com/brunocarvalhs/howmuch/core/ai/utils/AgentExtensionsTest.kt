package br.com.brunocarvalhs.howmuch.core.ai.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentExtensionsTest {

    @Test
    fun `getString trims and unwraps surrounding quotes`() {
        assertEquals("milk", mapOf("k" to "\"milk\"").getString("k"))
        assertEquals("milk", mapOf("k" to " milk ").getString("k"))
    }

    @Test
    fun `getString returns null for missing or blank values`() {
        assertNull(emptyMap<String, Any?>().getString("k"))
        assertNull(mapOf("k" to "").getString("k"))
    }

    @Test
    fun `getInt reads numbers directly and parses strings`() {
        assertEquals(5, mapOf("k" to 5).getInt("k"))
        assertEquals(5, mapOf("k" to "5").getInt("k"))
        assertNull(mapOf("k" to "not-a-number").getInt("k"))
    }

    @Test
    fun `getDouble reads numbers directly and parses strings`() {
        assertEquals(5.5, mapOf("k" to 5.5).getDouble("k"))
        assertEquals(5.5, mapOf("k" to "5.5").getDouble("k"))
        assertNull(mapOf("k" to "not-a-number").getDouble("k"))
    }

    @Test
    fun `getBoolean reads booleans directly`() {
        assertEquals(true, mapOf("k" to true).getBoolean("k"))
        assertEquals(false, mapOf("k" to false).getBoolean("k"))
    }

    @Test
    fun `getBoolean parses common string forms in Portuguese and English`() {
        assertEquals(true, mapOf("k" to "sim").getBoolean("k"))
        assertEquals(true, mapOf("k" to "YES").getBoolean("k"))
        assertEquals(true, mapOf("k" to "1").getBoolean("k"))
        assertEquals(false, mapOf("k" to "não").getBoolean("k"))
        assertEquals(false, mapOf("k" to "0").getBoolean("k"))
        assertNull(mapOf("k" to "maybe").getBoolean("k"))
    }

    @Test
    fun `getList reads a real list and strips quotes from each item`() {
        assertEquals(listOf("a", "b"), mapOf("k" to listOf("\"a\"", "b")).getList("k"))
    }

    @Test
    fun `getList splits a comma-separated string`() {
        assertEquals(listOf("a", "b", "c"), mapOf("k" to "a, b, c").getList("k"))
    }

    @Test
    fun `getList returns empty list when key is missing`() {
        assertEquals(emptyList<String>(), emptyMap<String, Any?>().getList("k"))
    }

    @Test
    fun `getMap reads a real map and strips quotes from keys and values`() {
        val result = mapOf("k" to mapOf("\"role\"" to "\"OWNER\"")).getMap("k")

        assertEquals(mapOf("role" to "OWNER"), result)
    }

    @Test
    fun `getMap returns empty map when value is not a map`() {
        assertTrue(mapOf("k" to "not-a-map").getMap("k").isEmpty())
    }
}
