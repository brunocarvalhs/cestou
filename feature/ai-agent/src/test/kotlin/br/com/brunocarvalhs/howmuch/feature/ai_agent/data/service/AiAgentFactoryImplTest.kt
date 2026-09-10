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

private fun FeatureFlagService.stubFlags(geminiEnabled: Boolean, openRouterEnabled: Boolean) {
    every { isEnabled(FeatureFlagKeys.AI_GEMINI_ENABLED, default = true) } returns geminiEnabled
    every { isEnabled(FeatureFlagKeys.AI_OPENROUTER_ENABLED, default = true) } returns openRouterEnabled
}

/** Explicit stub (never relaxed) so a test that forgets to configure the remote value fails loudly
 * instead of silently exercising the empty-key path.
 */
private fun RemoteVariableService.stubRemoteKey(value: String) {
    every {
        getString(key = RemoteVariableKeys.GEMINI_API_KEY, default = BuildConfig.GEMINI_API_KEY)
    } returns value
}

/** Same as [stubRemoteKey], for the OpenRouter remote key. `create()` reads both keys on every
 * call regardless of the selected provider, so this must be stubbed alongside [stubRemoteKey].
 */
private fun RemoteVariableService.stubRemoteOpenRouterKey(value: String) {
    every {
        getString(key = RemoteVariableKeys.OPEN_ROUTER_API_KEY, default = BuildConfig.OPEN_ROUTER_API_KEY)
    } returns value
}

/** Stubs both remote keys with their respective `BuildConfig` fallback, mirroring an
 * unfetched/unactivated Remote Config console. Used by tests that don't exercise rotation.
 */
private fun RemoteVariableService.stubDefaultRemoteKeys() {
    stubRemoteKey(BuildConfig.GEMINI_API_KEY)
    stubRemoteOpenRouterKey(BuildConfig.OPEN_ROUTER_API_KEY)
}

/** Reads the private `apiKey` field the GeminiAiAgent was actually constructed with. */
private fun GeminiAiAgent.actualApiKey(): String {
    val field: Field = GeminiAiAgent::class.java.getDeclaredField("apiKey")
    field.isAccessible = true
    return field.get(this) as String
}

/** Reads the private `apiKey` field the OpenRouterAiAgent was actually constructed with. */
private fun OpenRouterAiAgent.actualApiKey(): String {
    val field: Field = OpenRouterAiAgent::class.java.getDeclaredField("apiKey")
    field.isAccessible = true
    return field.get(this) as String
}

class AiAgentFactoryImplTest {

    private val session = mockk<AiAgentSession>(relaxed = true)
    private val registry = mockk<AgentRegistry>(relaxed = true)
    private val featureFlagService = mockk<FeatureFlagService>()
    private val remoteVariableService = mockk<RemoteVariableService>()
    private val factory = AiAgentFactoryImpl(session, registry, featureFlagService, remoteVariableService)

    @Test
    fun `create returns a FallbackAiAgent when no explicit provider is chosen and both are enabled`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is FallbackAiAgent)
    }

    @Test
    fun `create returns GeminiAiAgent when the gemini provider is explicitly selected`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = "gemini"))

        assertTrue(agent is GeminiAiAgent)
    }

    @Test
    fun `create returns OpenRouterAiAgent when the openrouter provider is explicitly selected`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = "openrouter"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create falls back to the other provider when the selected one is disabled`() {
        featureFlagService.stubFlags(geminiEnabled = false, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = "gemini"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create returns NoAiProviderAvailableAgent when both providers are disabled`() {
        featureFlagService.stubFlags(geminiEnabled = false, openRouterEnabled = false)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is NoAiProviderAvailableAgent)
    }

    @Test
    fun `create returns the single enabled provider when the other is disabled in auto mode`() {
        featureFlagService.stubFlags(geminiEnabled = false, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = "auto"))

        assertTrue(agent is OpenRouterAiAgent)
    }

    @Test
    fun `create builds GeminiAiAgent with the remote key when Remote Config returns a valid value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey("remote-rotated-key")
        remoteVariableService.stubRemoteOpenRouterKey(BuildConfig.OPEN_ROUTER_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "gemini")) as GeminiAiAgent

        assertEquals("remote-rotated-key", agent.actualApiKey())
    }

    @Test
    fun `create builds GeminiAiAgent with the BuildConfig key when Remote Config has no value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        // Mirrors FirebaseRemoteConfigService's real fallback contract: an unfetched/unactivated
        // key returns the caller-supplied default.
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = "gemini")) as GeminiAiAgent

        assertEquals(BuildConfig.GEMINI_API_KEY, agent.actualApiKey())
    }

    @Test
    fun `create falls back to the BuildConfig key when Remote Config returns a blank value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey("   ")
        remoteVariableService.stubRemoteOpenRouterKey(BuildConfig.OPEN_ROUTER_API_KEY)

        val agent = factory.create(AppSettings(aiProvider = "gemini")) as GeminiAiAgent

        assertEquals(BuildConfig.GEMINI_API_KEY, agent.actualApiKey())
    }

    @Test
    fun `create builds OpenRouterAiAgent with the remote key when Remote Config returns a valid value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey(BuildConfig.GEMINI_API_KEY)
        remoteVariableService.stubRemoteOpenRouterKey("remote-rotated-openrouter-key")

        val agent = factory.create(AppSettings(aiProvider = "openrouter")) as OpenRouterAiAgent

        assertEquals("remote-rotated-openrouter-key", agent.actualApiKey())
    }

    @Test
    fun `create falls back to the BuildConfig key when Remote Config returns a blank OpenRouter value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey(BuildConfig.GEMINI_API_KEY)
        remoteVariableService.stubRemoteOpenRouterKey("   ")

        val agent = factory.create(AppSettings(aiProvider = "openrouter")) as OpenRouterAiAgent

        assertEquals(BuildConfig.OPEN_ROUTER_API_KEY, agent.actualApiKey())
    }
}
