package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.FunctionCall
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the AD-010 gate at [OpenRouterAiAgent.executeToolCall]: the single chokepoint every
 * OpenRouter tool call passes through before an [br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction]
 * actually runs.
 */
class OpenRouterAiAgentGatingTest {

    private val registry = mockk<AgentRegistry>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()

    private fun agentWithStatus(status: SubscriptionStatus): OpenRouterAiAgent {
        every { subscriptionRepository.observeStatus() } returns flowOf(status)
        return OpenRouterAiAgent(
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

        val result = agent.executeToolCall(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertEquals("Essa ação é exclusiva do plano Pro.", result)
        assertFalse(action.executed)
    }

    @Test
    fun `runs a requiresPro action when the user is PRO`() = runTest {
        val action = FakeAgentAction(requiresPro = true)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.PRO)

        agent.executeToolCall(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertTrue(action.executed)
    }

    @Test
    fun `runs a non-pro action regardless of subscription status`() = runTest {
        val action = FakeAgentAction(requiresPro = false)
        every { registry.find("fake_action") } returns action
        val agent = agentWithStatus(SubscriptionStatus.FREE)

        agent.executeToolCall(FunctionCall(name = "fake_action", arguments = "{}"), emptyMap())

        assertTrue(action.executed)
    }
}
