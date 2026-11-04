package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import br.com.brunocarvalhs.howmuch.core.ai.model.AiHistoryTurn
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

    private val request = ApprovalRequest(actionId = "delete_shopping_list", summary = "Mercado")

    @Test
    fun `an approval request is published while it waits and cleared after`() = runTest {
        val session = AgentSession()
        val result = async { session.awaitApproval(request) }
        runCurrent()

        assertEquals(request, session.pendingApproval.value)

        session.resolveApproval(ApprovalDecision.APPROVED)

        assertEquals(ApprovalDecision.APPROVED, result.await())
        assertNull(session.pendingApproval.value)
    }

    @Test
    fun `a rejection comes back as rejected`() = runTest {
        val session = AgentSession()
        val result = async { session.awaitApproval(request) }
        runCurrent()

        session.resolveApproval(ApprovalDecision.REJECTED)

        assertEquals(ApprovalDecision.REJECTED, result.await())
    }

    @Test
    fun `leaving the chat with the card open counts as dismissed`() = runTest {
        val session = AgentSession()
        val result = async { session.awaitApproval(request) }
        runCurrent()

        session.resolveApproval(ApprovalDecision.DISMISSED)

        assertEquals(ApprovalDecision.DISMISSED, result.await())
        assertFalse(ApprovalDecision.DISMISSED.isApproved)
    }

    @Test
    fun `no answer in time becomes a timeout and clears the request`() = runTest {
        val session = AgentSession()
        val result = async { session.awaitApproval(request, timeoutMillis = 1_000) }
        runCurrent()

        advanceTimeBy(1_001)

        assertEquals(ApprovalDecision.TIMEOUT, result.await())
        assertNull(session.pendingApproval.value)
    }

    @Test
    fun `cancelling the turn clears the pending request`() = runTest {
        val session = AgentSession()
        val result = async { session.awaitApproval(request) }
        runCurrent()

        result.cancel()
        runCurrent()

        assertNull(session.pendingApproval.value)
    }

    @Test
    fun `a second request waits for the first one to finish`() = runTest {
        val session = AgentSession()
        val second = ApprovalRequest(actionId = "clear_purchased_products", summary = null)
        val firstResult = async { session.awaitApproval(request) }
        runCurrent()
        val secondResult = async { session.awaitApproval(second) }
        runCurrent()

        assertEquals(request, session.pendingApproval.value)

        session.resolveApproval(ApprovalDecision.APPROVED)
        runCurrent()

        assertEquals(second, session.pendingApproval.value)
        session.resolveApproval(ApprovalDecision.REJECTED)

        assertEquals(ApprovalDecision.APPROVED, firstResult.await())
        assertEquals(ApprovalDecision.REJECTED, secondResult.await())
    }

    @Test
    fun `resolving without an open request does nothing`() {
        val session = AgentSession()

        session.resolveApproval(ApprovalDecision.APPROVED)

        assertNull(session.pendingApproval.value)
    }
}
