package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.FunctionCall
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the AD-010 gate at [BaseAgent.executeAction] (sdd-026), reached here through
 * [OpenRouterAgent.toToolCallContent] — the adapter that converts the OpenAI-compatible tool
 * call into the shared executor's shape.
 */
class OpenRouterAgentGatingTest {

    private val registry = mockk<AgentRegistry>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val session = mockk<AiSession>(relaxed = true)

    private fun agentWithStatus(status: SubscriptionStatus): OpenRouterAgent {
        every { subscriptionRepository.observeStatus() } returns flowOf(status)
        return OpenRouterAgent(
            dependencies = AgentDependencies(
                session = session,
                crashReporter = mockk<CrashReporter>(relaxed = true),
                subscriptionRepository = subscriptionRepository,
                registry = registry
            ),
            httpClientFactory = mockk(relaxed = true)
        )
    }

    @Test
    fun `blocks a requiresPro action when the user is FREE`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        val result = agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertEquals("Essa ação é exclusiva do plano Pro.", result)
        assertFalse(action.executed)
    }

    @Test
    fun `runs a requiresPro action when the user is PRO`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertTrue(action.executed)
    }

    @Test
    fun `runs a non-pro action regardless of subscription status`() = runTest {
        val action = FakeAgentAction(requiresPro = false)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertTrue(action.executed)
    }

    @Test
    fun `reports a failed action as an explicit error, never as success`() = runTest {
        val action = FakeAgentAction(result = Result.failure(Exception("not found")))
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        val result = agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertEquals("Erro: not found", result)
    }

    @Test
    fun `reports a Unit success as OK, not the literal kotlin-Unit`() = runTest {
        val action = FakeAgentAction(result = Result.success(Unit))
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        val result = agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertEquals("OK", result)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `a cancelled coroutine during tool execution propagates, instead of being reported as a tool error`() =
        runTest {
            val action = FakeAgentAction(throwable = kotlinx.coroutines.CancellationException("cancelled"))
            every { registry.find("fake_action") } returns action
            val agent = agentWithStatus(SubscriptionStatus.FREE)

            agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())
        }

    @Test
    fun `marks the session as having run a tool before dispatching it, even if it then fails`() = runTest {
        // sdd-012: FallbackAgent relies on this flag being set *before* the call, not after,
        // since a failed action can still have a partial side effect.
        val action = FakeAgentAction(result = Result.failure(Exception("boom")))
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        verify { session.toolExecutedThisTurn = true }
    }

    @Test
    fun `does not mark the session when the action is Pro-gated and denied`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.toToolCallContent(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        verify(exactly = 0) { session.toolExecutedThisTurn = true }
    }
}
