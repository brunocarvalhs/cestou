package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteVariableKeys
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AiAgentSession
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field

class AiAgentFactoryImplTest {

    private val session = mockk<AiAgentSession>(relaxed = true)
    private val registry = mockk<AgentRegistry>(relaxed = true)
    private val featureFlagService = mockk<FeatureFlagService>()
    private val remoteVariableService = mockk<RemoteVariableService>()
    private val factory = AiAgentFactoryImpl(session, registry, featureFlagService, remoteVariableService)

    private fun stubFlags(geminiEnabled: Boolean, openRouterEnabled: Boolean) {
        every { featureFlagService.isEnabled(FeatureFlagKeys.AI_GEMINI_ENABLED, default = true) } returns geminiEnabled
        every {
            featureFlagService.isEnabled(FeatureFlagKeys.AI_OPENROUTER_ENABLED, default = true)
        } returns openRouterEnabled
    }

    /** Explicit stub (never relaxed) so a test that forgets to configure the remote value fails loudly
     * instead of silently exercising the empty-key path.
     */
    private fun stubRemoteKey(value: String) {
        every {
            remoteVariableService.getString(
                key = RemoteVariableKeys.GEMINI_API_KEY,
                default = BuildConfig.GEMINI_API_KEY
            )
        } returns value
    }

    /** Reads the private `apiKey` field the GeminiAiAgent was actually constructed with. */
    private fun GeminiAiAgent.actualApiKey(): String {
        val field: Field = GeminiAiAgent::class.java.getDeclaredField("apiKey")
        field.isAccessible = true
        return field.get(this) as String
    }

    @Test
    fun `create returns a FallbackAiAgent when no explicit provider is chosen and both are enabled`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)
        stubRemoteKey(BuildConfig.GEMINI_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is FallbackAiAgent)
    }

    @Test
    fun `create returns GeminiAiAgent when the gemini provider is explicitly selected`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)
        stubRemoteKey(BuildConfig.GEMINI_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "gemini"))

        assertTrue(agent is GeminiAiAgent)
    }

    @Test
    fun `create returns OpenRouterAiAgent when the openrouter provider is explicitly selected`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)
        stubRemoteKey(BuildConfig.GEMINI_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "openrouter"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create falls back to the other provider when the selected one is disabled`() {
        stubFlags(geminiEnabled = false, openRouterEnabled = true)
        stubRemoteKey(BuildConfig.GEMINI_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "gemini"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create returns NoAiProviderAvailableAgent when both providers are disabled`() {
        stubFlags(geminiEnabled = false, openRouterEnabled = false)
        stubRemoteKey(BuildConfig.GEMINI_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is NoAiProviderAvailableAgent)
    }

    @Test
    fun `create returns the single enabled provider when the other is disabled in auto mode`() {
        stubFlags(geminiEnabled = false, openRouterEnabled = true)
        stubRemoteKey(BuildConfig.GEMINI_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create builds GeminiAiAgent with the remote key when Remote Config returns a valid value`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)
        stubRemoteKey("remote-rotated-key")

        val agent = factory.create(AppSettings(aiProvider = "gemini")) as GeminiAiAgent

        assertEquals("remote-rotated-key", agent.actualApiKey())
    }

    @Test
    fun `create builds GeminiAiAgent with the BuildConfig key when Remote Config has no value`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)
        // Mirrors FirebaseRemoteConfigService's real fallback contract: an unfetched/unactivated
        // key returns the caller-supplied default.
        stubRemoteKey(BuildConfig.GEMINI_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "gemini")) as GeminiAiAgent

        assertEquals(BuildConfig.GEMINI_API_KEY, agent.actualApiKey())
    }

    @Test
    fun `create falls back to the BuildConfig key when Remote Config returns a blank value`() {
        stubFlags(geminiEnabled = true, openRouterEnabled = true)
        stubRemoteKey("   ")

        val agent = factory.create(AppSettings(aiProvider = "gemini")) as GeminiAiAgent

        assertEquals(BuildConfig.GEMINI_API_KEY, agent.actualApiKey())
    }
}
