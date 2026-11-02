package br.com.brunocarvalhs.howmuch.core.common.wearable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingCodeHolderTest {

    @Test
    fun `pairing code holder stores and retrieves code`() {
        val holder = PairingCodeHolder()
        assertNull(holder.currentCode)

        holder.currentCode = "123456"
        assertEquals("123456", holder.currentCode)

        holder.currentCode = null
        assertNull(holder.currentCode)
    }
}
