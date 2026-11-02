package br.com.brunocarvalhs.howmuch.core.common.wearable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WearablePathsTest {

    @Test
    fun `encode and decode auth pair works correctly`() {
        val encoded = WearablePaths.encodeAuthPair("user-1", "token-abc")
        assertNotNull(encoded)

        val decoded = WearablePaths.decodeAuthPair(encoded)
        assertNotNull(decoded)
        assertEquals("user-1", decoded?.first)
        assertEquals("token-abc", decoded?.second)
    }

    @Test
    fun `decode malformed auth pair returns null`() {
        val malformed = "single-part".toByteArray()
        assertNull(WearablePaths.decodeAuthPair(malformed))
    }
}
