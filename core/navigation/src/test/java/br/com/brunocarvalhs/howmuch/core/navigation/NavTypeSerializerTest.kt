package br.com.brunocarvalhs.howmuch.core.navigation

import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Serializable
private data class TestPayload(val id: String, val quantity: Int)

class NavTypeSerializerTest {

    private val navType = navTypeSerializer<TestPayload>()
    private val nullableNavType = navTypeSerializerNullable<TestPayload>()
    private val listNavType = navTypeListSerializer<TestPayload>()

    @Test
    fun `put encodes the value as JSON into the bundle`() {
        val bundle = mockk<Bundle>(relaxed = true)
        val valueSlot = slot<String>()

        navType.put(bundle, "payload", TestPayload("p1", 2))

        verify { bundle.putString("payload", capture(valueSlot)) }
        assertEquals("""{"id":"p1","quantity":2}""", valueSlot.captured)
    }

    @Test
    fun `get decodes JSON stored in the bundle back into the value`() {
        val bundle = mockk<Bundle>()
        every { bundle.getString("payload") } returns """{"id":"p1","quantity":2}"""

        assertEquals(TestPayload("p1", 2), navType.get(bundle, "payload"))
    }

    @Test
    fun `get returns null when the bundle has no value for the key`() {
        val bundle = mockk<Bundle>()
        every { bundle.getString("payload") } returns null

        assertNull(navType.get(bundle, "payload"))
    }

    @Test
    fun `parseValue url-decodes then deserializes the route argument`() {
        val encoded = java.net.URLEncoder.encode("""{"id":"p1","quantity":2}""", "UTF-8")

        assertEquals(TestPayload("p1", 2), navType.parseValue(encoded))
    }

    @Test
    fun `serializeAsValue url-encodes the serialized JSON`() {
        val serialized = navType.serializeAsValue(TestPayload("p1", 2))

        assertEquals(TestPayload("p1", 2), navType.parseValue(serialized))
    }

    @Test
    fun `nullable navType round-trips a null value as the literal string null`() {
        assertEquals("null", nullableNavType.serializeAsValue(null))
        assertNull(nullableNavType.parseValue("null"))
    }

    @Test
    fun `nullable navType round-trips a non-null value`() {
        val serialized = nullableNavType.serializeAsValue(TestPayload("p1", 2))

        assertEquals(TestPayload("p1", 2), nullableNavType.parseValue(serialized))
    }

    @Test
    fun `list navType round-trips a list of values`() {
        val payloads = listOf(TestPayload("p1", 1), TestPayload("p2", 2))
        val serialized = listNavType.serializeAsValue(payloads)

        assertEquals(payloads, listNavType.parseValue(serialized))
    }
}
