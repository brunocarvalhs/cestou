package br.com.brunocarvalhs.howmuch.core.common.wearable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingSessionHolderTest {

    private val holder = PairingSessionHolder()

    @Test
    fun `a sign-in is accepted only after the watch user confirmed the active session`() {
        holder.startSession("session-1")
        assertFalse(holder.isConfirmed("session-1"))

        assertTrue(holder.markWatchConfirmed("session-1"))

        assertTrue(holder.isConfirmed("session-1"))
    }

    @Test
    fun `confirming a session that is not active is refused`() {
        holder.startSession("session-1")

        assertFalse(holder.markWatchConfirmed("session-old"))

        assertFalse(holder.isConfirmed("session-1"))
    }

    @Test
    fun `a sign-in for a stale session is never confirmed`() {
        holder.startSession("session-1")
        holder.markWatchConfirmed("session-1")

        holder.startSession("session-2")

        assertFalse(holder.isConfirmed("session-1"))
        assertFalse(holder.isConfirmed("session-2"))
    }

    @Test
    fun `ending the session clears the active id and the confirmation`() {
        holder.startSession("session-1")
        holder.markWatchConfirmed("session-1")

        holder.endSession()

        assertNull(holder.activeSessionId)
        assertFalse(holder.isConfirmed("session-1"))
    }

    @Test
    fun `starting a session exposes its id`() {
        holder.startSession("session-1")

        assertEquals("session-1", holder.activeSessionId)
    }
}
