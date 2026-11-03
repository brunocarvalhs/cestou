package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentSessionTest {

    @Test
    fun `toolExecutedThisTurn starts false`() {
        val session = AgentSession()

        assertFalse(session.toolExecutedThisTurn)
    }

    @Test
    fun `toolExecutedThisTurn is mutable and independent of history`() {
        val session = AgentSession()

        session.toolExecutedThisTurn = true

        assertTrue(session.toolExecutedThisTurn)
        assertTrue(session.history.isEmpty())
    }

    @Test
    fun `clearHistory clears history without touching toolExecutedThisTurn`() {
        val session = AgentSession()
        session.toolExecutedThisTurn = true
        session.history.add(com.google.ai.client.generativeai.type.Content(role = "user", parts = emptyList()))

        session.clearHistory()

        assertTrue(session.history.isEmpty())
        assertTrue(session.toolExecutedThisTurn)
    }

    @Test
    fun `telemetry fields start empty`() {
        val session = AgentSession()

        assertNull(session.providerUsed)
        assertEquals(0, session.toolCallsThisTurn)
        assertFalse(session.fallbackUsedThisTurn)
    }

    @Test
    fun `clearHistory keeps the telemetry of the last turn`() {
        val session = AgentSession()
        session.providerUsed = "gemini"
        session.toolCallsThisTurn = 2
        session.fallbackUsedThisTurn = true

        session.clearHistory()

        assertEquals("gemini", session.providerUsed)
        assertEquals(2, session.toolCallsThisTurn)
        assertTrue(session.fallbackUsedThisTurn)
    }
}
