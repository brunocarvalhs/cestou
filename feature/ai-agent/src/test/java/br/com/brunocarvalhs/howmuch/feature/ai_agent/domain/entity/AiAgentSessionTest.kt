package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAgentSessionTest {

    @Test
    fun `toolExecutedThisTurn starts false`() {
        val session = AiAgentSession()

        assertFalse(session.toolExecutedThisTurn)
    }

    @Test
    fun `toolExecutedThisTurn is mutable and independent of history`() {
        val session = AiAgentSession()

        session.toolExecutedThisTurn = true

        assertTrue(session.toolExecutedThisTurn)
        assertTrue(session.history.isEmpty())
    }

    @Test
    fun `clearHistory clears history without touching toolExecutedThisTurn`() {
        val session = AiAgentSession()
        session.toolExecutedThisTurn = true
        session.history.add(com.google.ai.client.generativeai.type.Content(role = "user", parts = emptyList()))

        session.clearHistory()

        assertTrue(session.history.isEmpty())
        assertTrue(session.toolExecutedThisTurn)
    }
}
