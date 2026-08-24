package br.com.brunocarvalhs.howmuch.core.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setup() {
        cryptoManager = CryptoManager()
    }

    @Test
    fun `encrypt then decrypt round-trips to the original value`() {
        val original = "milk and eggs"

        val encrypted = cryptoManager.encrypt(original)
        val decrypted = cryptoManager.decrypt(encrypted)

        assertNotEquals(original, encrypted)
        assertEquals(original, decrypted)
    }

    @Test
    fun `decrypt returns the input unchanged when it is not validly encoded`() {
        val notEncrypted = "plain text, not base64url"

        assertEquals(notEncrypted, cryptoManager.decrypt(notEncrypted))
    }

    @Test
    fun `encryptMap encrypts string values and leaves excluded keys untouched`() {
        val input = mapOf("title" to "Weekly Groceries", "id" to "list-1")

        val result = cryptoManager.encryptMap(input, excludedKeys = setOf("id"))

        assertEquals("list-1", result["id"])
        assertNotEquals("Weekly Groceries", result["title"])
    }

    @Test
    fun `encryptMap drops null values`() {
        val input = mapOf("title" to "Weekly Groceries", "description" to null)

        val result = cryptoManager.encryptMap(input, excludedKeys = emptySet())

        assertEquals(setOf("title"), result.keys)
    }

    @Test
    fun `encryptMap then decryptMap round-trips non-excluded values`() {
        val input = mapOf("title" to "Weekly Groceries", "price" to 42.5, "id" to "list-1")

        val encrypted = cryptoManager.encryptMap(input, excludedKeys = setOf("id"))
        val decrypted = cryptoManager.decryptMap(encrypted, excludedKeys = setOf("id"))

        assertEquals("Weekly Groceries", decrypted["title"])
        assertEquals(42.5, decrypted["price"])
        assertEquals("list-1", decrypted["id"])
    }

    @Test
    fun `encryptMap encrypts nested map and list values recursively`() {
        val input = mapOf(
            "roles" to mapOf("user-1" to "OWNER"),
            "users" to listOf("user-1", "user-2")
        )

        val encrypted = cryptoManager.encryptMap(input, excludedKeys = emptySet())
        val decrypted = cryptoManager.decryptMap(encrypted, excludedKeys = emptySet())

        assertEquals(input["roles"], decrypted["roles"])
        assertEquals(input["users"], decrypted["users"])
    }
}
