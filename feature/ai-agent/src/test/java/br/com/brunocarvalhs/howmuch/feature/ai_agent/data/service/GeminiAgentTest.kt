package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre a integração entre [GeminiAgent.sendMessage] e o [GeminiModelFactory] injetado via Hilt
 * (sdd-026, A-03) — antes o `GenerativeModel` era montado inline, sem seam de injeção. O gate de
 * dispatch em si (Pro/registry/erro de tool) já é coberto por [GeminiAgentGatingTest].
 */
class GeminiAgentTest {

    private val session = mockk<AiSession>(relaxed = true)
    private val registry = mockk<AgentRegistry>(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
    private val modelFactory = mockk<GeminiModelFactory>()

    private val agent = GeminiAgent(
        dependencies = AgentDependencies(
            session = session,
            crashReporter = crashReporter,
            subscriptionRepository = subscriptionRepository,
            registry = registry,
            systemPrompt = "system prompt"
        ),
        modelFactory = modelFactory,
        modelName = "gemini-test",
        apiKey = "test-key"
    )

    @Test
    fun `sendMessage builds the model via the injected factory with the configured parameters`() = runTest {
        every { session.history } returns mutableListOf()
        every {
            modelFactory.create(
                modelName = eq("gemini-test"),
                apiKey = eq("test-key"),
                systemPrompt = eq("system prompt"),
                tools = any()
            )
        } throws IllegalStateException("boom")
        val context = mockk<AgentContext> { every { toMetadata() } returns emptyMap() }

        val failure = runCatching { agent.sendMessage("oi", context).toList() }

        assertTrue(failure.isFailure)
        verify(exactly = 1) {
            modelFactory.create(
                modelName = eq("gemini-test"),
                apiKey = eq("test-key"),
                systemPrompt = eq("system prompt"),
                tools = any()
            )
        }
    }

    @Test
    fun `sendMessage reports a transport failure to the crash reporter with the gemini provider tag`() = runTest {
        val generativeModel = mockk<GenerativeModel>(relaxed = true)
        val chat = mockk<Chat>(relaxed = true)
        every { modelFactory.create(any(), any(), any(), any()) } returns generativeModel
        every { generativeModel.startChat(any()) } returns chat
        coEvery { chat.sendMessage(any<String>()) } throws IllegalStateException("boom")
        every { session.history } returns mutableListOf()
        val context = mockk<AgentContext> { every { toMetadata() } returns emptyMap() }

        val failure = runCatching { agent.sendMessage("oi", context).toList() }

        assertTrue(failure.isFailure)
        verify {
            crashReporter.recordException(
                any(),
                extras = mapOf("provider" to "gemini", "model" to "gemini-test")
            )
        }
    }
}
