package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre a integração entre [OpenRouterAgent.sendMessage] e o [OpenRouterHttpClientFactory]
 * injetado via Hilt (sdd-026, A-03) — antes o [io.ktor.client.HttpClient] era montado inline, sem
 * seam de injeção. O gate de dispatch em si (Pro/registry/erro de tool) já é coberto por
 * [OpenRouterAgentGatingTest].
 */
class OpenRouterAgentTest {

    private val session = mockk<AiSession>(relaxed = true)
    private val registry = mockk<AgentRegistry>(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
    private val httpClientFactory = mockk<OpenRouterHttpClientFactory>()

    private val agent = OpenRouterAgent(
        dependencies = AgentDependencies(
            session = session,
            crashReporter = crashReporter,
            subscriptionRepository = subscriptionRepository,
            registry = registry,
            systemPrompt = "system prompt"
        ),
        httpClientFactory = httpClientFactory,
        model = "openrouter-test-model",
        apiKey = "test-key"
    )

    @Test
    fun `sendMessage builds the client via the injected factory and reports a failure to construct it`() = runTest {
        every { session.history } returns mutableListOf()
        every { httpClientFactory.create(any()) } throws IllegalStateException("boom")
        val context = mockk<AgentContext> { every { toMetadata() } returns emptyMap() }

        val failure = runCatching { agent.sendMessage("oi", context).toList() }

        assertTrue(failure.isFailure)
        verify(exactly = 1) { httpClientFactory.create(any()) }
        verify {
            crashReporter.recordException(
                any(),
                extras = mapOf("provider" to "openrouter", "model" to "openrouter-test-model")
            )
        }
    }
}
