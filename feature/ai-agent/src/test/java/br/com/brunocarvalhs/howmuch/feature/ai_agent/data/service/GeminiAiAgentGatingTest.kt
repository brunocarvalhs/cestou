package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import com.google.ai.client.generativeai.type.FunctionCallPart
import io.mockk.every
import io.mockk.mockk
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
 * Covers the AD-010 gate at [GeminiAiAgent.executeFunctionCall]: the single chokepoint every
 * Gemini function call passes through before an [br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction]
 * actually runs. Runs under Robolectric because [GeminiAiAgent.executeFunctionCall] builds its
 * response with the real `org.json.JSONObject`, which throws "not mocked" under a plain JVM
 * unit test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiAiAgentGatingTest {

    private val registry = mockk<AgentRegistry>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()

    private fun agentWithStatus(status: SubscriptionStatus): GeminiAiAgent {
        every { subscriptionRepository.observeStatus() } returns flowOf(status)
        return GeminiAiAgent(
            dependencies = AiAgentDependencies(
                session = mockk<AiSession>(relaxed = true),
                crashReporter = mockk<CrashReporter>(relaxed = true),
                subscriptionRepository = subscriptionRepository,
                registry = registry
            )
        )
    }

    @Test
    fun `blocks a requiresPro action when the user is FREE`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        val response = agent.executeFunctionCall(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertEquals("Essa ação é exclusiva do plano Pro.", response.response.getString("result"))
        assertFalse(action.executed)
    }

    @Test
    fun `runs a requiresPro action when the user is PRO`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        agent.executeFunctionCall(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertTrue(action.executed)
    }

    @Test
    fun `runs a non-pro action regardless of subscription status`() = runTest {
        val action = FakeAgentAction(requiresPro = false)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.executeFunctionCall(FunctionCallPart("fake_action", emptyMap()), emptyMap())

        assertTrue(action.executed)
    }
}
