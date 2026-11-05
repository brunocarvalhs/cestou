package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AgentSession
import com.google.ai.client.generativeai.type.FunctionCallPart
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the AD-010 gate at [BaseAgent.executeAction] (sdd-026), reached here through
 * [GeminiAgent.toFunctionResponsePart] — the adapter that converts the Gemini SDK's function
 * call into the shared executor's shape and back. Runs under Robolectric because
 * [GeminiAgent.toFunctionResponsePart] builds its response with the real `org.json.JSONObject`,
 * which throws "not mocked" under a plain JVM unit test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiAgentGatingTest {

    private val registry = mockk<AgentRegistry>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val session = mockk<AiSession>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private fun agentWithStatus(status: SubscriptionStatus): GeminiAgent {
        every { subscriptionRepository.observeStatus() } returns flowOf(status)
        return GeminiAgent(
            dependencies = AgentDependencies(
                session = session,
                crashReporter = mockk<CrashReporter>(relaxed = true),
                subscriptionRepository = subscriptionRepository,
                registry = registry,
                analyticsTracker = analyticsTracker
            ),
            modelFactory = mockk(relaxed = true)
        )
    }

    @Test
    fun `blocks a requiresPro action when the user is FREE`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        val response = agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertEquals("Essa ação é exclusiva do plano Pro.", response.response.getString("result"))
        assertFalse(action.executed)
    }

    @Test
    fun `runs a requiresPro action when the user is PRO`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertTrue(action.executed)
    }

    @Test
    fun `runs a non-pro action regardless of subscription status`() = runTest {
        val action = FakeAgentAction(requiresPro = false)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertTrue(action.executed)
    }

    @Test
    fun `reports a failed action as an explicit error, never as success`() = runTest {
        val action = FakeAgentAction(result = Result.failure(Exception("not found")))
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        val response = agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertEquals("Erro: not found", response.response.getString("result"))
    }

    @Test
    fun `reports a Unit success as OK, not the literal kotlin-Unit`() = runTest {
        val action = FakeAgentAction(result = Result.success(Unit))
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        val response = agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertEquals("OK", response.response.getString("result"))
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `a cancelled coroutine during tool execution propagates, instead of being reported as a tool error`() =
        runTest {
            val action = FakeAgentAction(throwable = kotlinx.coroutines.CancellationException("cancelled"))
            every { registry.find("fake_action") } returns action
            val agent = agentWithStatus(SubscriptionStatus.FREE)

            agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())
        }

    @Test
    fun `marks the session as having run a tool before dispatching it, even if it then fails`() = runTest {
        // sdd-012: FallbackAgent relies on this flag being set *before* the call, not after,
        // since a failed action can still have a partial side effect.
        val action = FakeAgentAction(result = Result.failure(Exception("boom")))
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        verify { session.toolExecutedThisTurn = true }
    }

    @Test
    fun `does not mark the session when the action is Pro-gated and denied`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        verify(exactly = 0) { session.toolExecutedThisTurn = true }
    }

    @Test
    fun `tracks a Pro-gated denial without running the action`() = runTest {
        every { registry.find("fake_action") } returns FakeAgentAction(requiresPro = true)

        agentWithStatus(SubscriptionStatus.FREE)
            .toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_TOOL_BLOCKED_PRO, mapOf("action_id" to "fake_action")) }
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.AI_TOOL_EXECUTED, any()) }
    }

    @Test
    fun `tracks a successful tool run with its action id, provider and duration`() = runTest {
        every { registry.find("fake_action") } returns FakeAgentAction()

        agentWithStatus(SubscriptionStatus.FREE)
            .toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_TOOL_EXECUTED,
                match {
                    it["action_id"] == "fake_action" && it["success"] == true &&
                        it["provider"] == "gemini" && it.containsKey("duration_ms")
                }
            )
        }
    }

    @Test
    fun `tracks a failed tool result as success false`() = runTest {
        every { registry.find("fake_action") } returns FakeAgentAction(result = Result.failure(Exception("x")))

        agentWithStatus(SubscriptionStatus.FREE)
            .toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_TOOL_EXECUTED, match { it["success"] == false }) }
    }

    private fun countingAgent(realSession: AgentSession, status: SubscriptionStatus): GeminiAgent {
        every { subscriptionRepository.observeStatus() } returns flowOf(status)
        return GeminiAgent(
            dependencies = AgentDependencies(
                session = realSession,
                crashReporter = mockk<CrashReporter>(relaxed = true),
                subscriptionRepository = subscriptionRepository,
                registry = registry,
                analyticsTracker = analyticsTracker
            ),
            modelFactory = mockk(relaxed = true)
        )
    }

    @Test
    fun `each executed tool adds one to the turn count that feeds tool_calls`() = runTest {
        val realSession = AgentSession()
        every { registry.find("fake_action") } returns FakeAgentAction()
        val agent = countingAgent(realSession, SubscriptionStatus.PRO)

        agent.executeAction("fake_action", emptyMap(), emptyMap())
        agent.executeAction("fake_action", emptyMap(), emptyMap())

        assertEquals(2, realSession.toolCallsThisTurn)
    }

    @Test
    fun `a tool that throws still counts, since it may have changed data`() = runTest {
        val realSession = AgentSession()
        every { registry.find("fake_action") } returns FakeAgentAction(throwable = IllegalStateException("boom"))

        countingAgent(realSession, SubscriptionStatus.PRO).executeAction("fake_action", emptyMap(), emptyMap())

        assertEquals(1, realSession.toolCallsThisTurn)
    }

    @Test
    fun `a tool blocked by the Pro gate does not count`() = runTest {
        val realSession = AgentSession()
        every { registry.find("fake_action") } returns FakeAgentAction(requiresPro = true)

        countingAgent(realSession, SubscriptionStatus.FREE).executeAction("fake_action", emptyMap(), emptyMap())

        assertEquals(0, realSession.toolCallsThisTurn)
    }

    @Test
    fun `an unknown action does not count`() = runTest {
        val realSession = AgentSession()
        every { registry.find("missing") } returns null

        countingAgent(realSession, SubscriptionStatus.PRO).executeAction("missing", emptyMap(), emptyMap())

        assertEquals(0, realSession.toolCallsThisTurn)
    }

    private fun approvalAnswer(decision: ApprovalDecision) =
        coEvery { session.awaitApproval(any(), any()) } returns decision

    @Test
    fun `an irreversible action is not executed when the user rejects the card`() = runTest {
        val action = FakeAgentAction(requiresConfirmation = true)
        every { registry.find("fake_action") } returns action
        approvalAnswer(ApprovalDecision.REJECTED)
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        val response = agent.toFunctionResponsePart(FunctionCallPart("fake_action", mapOf("id" to "1")), emptyMap())

        assertTrue(response.response.getString("result").startsWith("NÃO EXECUTADO"))
        assertFalse(action.executed)
        coVerify { session.awaitApproval(match { it.actionId == "fake_action" }, any()) }
    }

    @Test
    fun `an irreversible action is not executed when the user does not answer in time`() = runTest {
        val action = FakeAgentAction(requiresConfirmation = true)
        every { registry.find("fake_action") } returns action
        approvalAnswer(ApprovalDecision.TIMEOUT)

        agentWithStatus(SubscriptionStatus.PRO)
            .toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertFalse(action.executed)
    }

    @Test
    fun `an irreversible action runs right after the user approves the card`() = runTest {
        val action = FakeAgentAction(requiresConfirmation = true)
        every { registry.find("fake_action") } returns action
        approvalAnswer(ApprovalDecision.APPROVED)
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        val response = agent.toFunctionResponsePart(FunctionCallPart("fake_action", mapOf("id" to "1")), emptyMap())

        assertTrue(action.executed)
        assertFalse(response.response.getString("result").startsWith("NÃO EXECUTADO"))
    }

    @Test
    fun `the Pro gate comes before the approval card`() = runTest {
        val action = FakeAgentAction(requiresConfirmation = true, requiresPro = true)
        every { registry.find("fake_action") } returns action
        approvalAnswer(ApprovalDecision.APPROVED)

        agentWithStatus(SubscriptionStatus.FREE)
            .toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertFalse(action.executed)
        coVerify(exactly = 0) { session.awaitApproval(any(), any()) }
    }

    @Test
    fun `an action that needs no confirmation never opens the card`() = runTest {
        val action = FakeAgentAction()
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertTrue(action.executed)
        coVerify(exactly = 0) { session.awaitApproval(any(), any()) }
    }

    @Test
    fun `the running action is published while it executes and cleared after`() = runTest {
        val action = FakeAgentAction()
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        agent.toFunctionResponsePart(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        verifyOrder {
            session.setActiveAction("fake_action")
            session.setActiveAction(null)
        }
    }
}
