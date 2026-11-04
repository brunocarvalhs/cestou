package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import br.com.brunocarvalhs.howmuch.core.ai.model.AiHistoryTurn
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

    @Test
    fun `restoreHistory rebuilds the model memory in order`() {
        val session = AgentSession()

        session.restoreHistory(listOf(AiHistoryTurn(true, "add milk"), AiHistoryTurn(false, "done")))

        assertEquals(listOf("user", "model"), session.history.map { it.role })
    }

    @Test
    fun `restoreHistory merges consecutive turns of the same author`() {
        val session = AgentSession()

        session.restoreHistory(
            listOf(AiHistoryTurn(true, "one"), AiHistoryTurn(true, "two"), AiHistoryTurn(false, "ok"))
        )

        assertEquals(listOf("user", "model"), session.history.map { it.role })
    }

    @Test
    fun `a confirmation requested in a turn is not valid in the same turn`() {
        val session = AgentSession()
        session.beginTurn()
        session.requestConfirmation("delete(a)")

        assertFalse(session.consumeConfirmation("delete(a)"))
    }

    @Test
    fun `a confirmation requested earlier is valid once after the user answers`() {
        val session = AgentSession()
        session.beginTurn()
        session.requestConfirmation("delete(a)")
        session.beginTurn()

        assertTrue(session.consumeConfirmation("delete(a)"))
        assertFalse(session.consumeConfirmation("delete(a)"))
    }

    @Test
    fun `a confirmation for other arguments is not accepted`() {
        val session = AgentSession()
        session.beginTurn()
        session.requestConfirmation("delete(a)")
        session.beginTurn()

        assertFalse(session.consumeConfirmation("delete(b)"))
    }
}
