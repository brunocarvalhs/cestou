package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import com.google.ai.client.generativeai.type.FunctionCallPart
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
 * [GeminiAiAgent.toFunctionResponsePart] — the adapter that converts the Gemini SDK's function
 * call into the shared executor's shape and back. Runs under Robolectric because
 * [GeminiAiAgent.toFunctionResponsePart] builds its response with the real `org.json.JSONObject`,
 * which throws "not mocked" under a plain JVM unit test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiAiAgentGatingTest {

    private val registry = mockk<AgentRegistry>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val session = mockk<AiSession>(relaxed = true)

    private fun agentWithStatus(status: SubscriptionStatus): GeminiAiAgent {
        every { subscriptionRepository.observeStatus() } returns flowOf(status)
        return GeminiAiAgent(
            dependencies = AiAgentDependencies(
                session = session,
                crashReporter = mockk<CrashReporter>(relaxed = true),
                subscriptionRepository = subscriptionRepository,
                registry = registry
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
        // sdd-012: FallbackAiAgent relies on this flag being set *before* the call, not after,
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
}
