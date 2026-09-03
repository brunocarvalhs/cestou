package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AiAgentSession
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAgentFactoryImplTest {

    private val session = mockk<AiAgentSession>(relaxed = true)
    private val registry = mockk<AgentRegistry>(relaxed = true)
    private val featureFlagService = mockk<FeatureFlagService>()
    private val factory = AiAgentFactoryImpl(session, registry, featureFlagService)

    private fun stubFlags(geminiEnabled: Boolean, openRouterEnabled: Boolean) {
        every { featureFlagService.isEnabled(FeatureFlagKeys.AI_GEMINI_ENABLED, default = true) } returns geminiEnabled
        every {
            featureFlagService.isEnabled(FeatureFlagKeys.AI_OPENROUTER_ENABLED, default = true)
        } returns openRouterEnabled
    }

    @Test
    fun `create returns a FallbackAiAgent when no explicit provider is chosen and both are enabled`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is FallbackAiAgent)
    }

    @Test
    fun `create returns GeminiAiAgent when the gemini provider is explicitly selected`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)

        val agent = factory.create(AppSettings(aiProvider = "gemini"))

        assertTrue(agent is GeminiAiAgent)
    }

    @Test
    fun `create returns OpenRouterAiAgent when the openrouter provider is explicitly selected`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)

        val agent = factory.create(AppSettings(aiProvider = "openrouter"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create falls back to the other provider when the selected one is disabled`() {
        stubFlags(geminiEnabled = false, openRouterEnabled = true)

        val agent = factory.create(AppSettings(aiProvider = "gemini"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create returns NoAiProviderAvailableAgent when both providers are disabled`() {
        stubFlags(geminiEnabled = false, openRouterEnabled = false)

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is NoAiProviderAvailableAgent)
    }

    @Test
    fun `create returns the single enabled provider when the other is disabled in auto mode`() {
        stubFlags(geminiEnabled = false, openRouterEnabled = true)

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is OpenRouterAiAgent)
    }
}
