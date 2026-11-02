// Lots of small, single-purpose stub/reflection helpers below plus a wide combinatorial test
// matrix in the class itself — both trip TooManyFunctions, neither is actually a design smell.
@file:Suppress("TooManyFunctions")

package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.AiProvider
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteVariableKeys
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AgentSession
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

/** `create()` also reads the remote system prompt on every call; stub it with the compiled-in
 * default unless a test is specifically exercising prompt rotation.
 */
private fun RemoteVariableService.stubRemoteSystemPrompt(value: String = SystemPrompts.CESTOU_ASSISTANT) {
    every {
        getString(key = RemoteVariableKeys.AI_SYSTEM_PROMPT, default = SystemPrompts.CESTOU_ASSISTANT)
    } returns value
}

/** Stubs both remote keys with their respective `BuildConfig` fallback, mirroring an
 * unfetched/unactivated Remote Config console. Used by tests that don't exercise rotation.
 */
private fun RemoteVariableService.stubDefaultRemoteKeys() {
    stubRemoteKey(BuildConfig.GEMINI_API_KEY)
    stubRemoteOpenRouterKey(BuildConfig.OPEN_ROUTER_API_KEY)
    stubRemoteSystemPrompt()
}

/** Walks up the class hierarchy to find [name] — needed for fields that now live in [BaseAgent]
 * (e.g. `systemPrompt`), since `getDeclaredField` only looks at the exact class's own fields. */
private fun Any.declaredFieldRecursive(name: String): Field {
    var type: Class<*>? = this::class.java
    while (type != null) {
        try {
            return type.getDeclaredField(name).also { it.isAccessible = true }
        } catch (e: NoSuchFieldException) {
            type = type.superclass
        }
    }
    throw NoSuchFieldException(name)
}

/** Reads the private `apiKey` field the GeminiAgent was actually constructed with. */
private fun GeminiAgent.actualApiKey(): String {
    val field: Field = GeminiAgent::class.java.getDeclaredField("apiKey")
    field.isAccessible = true
    return field.get(this) as String
}

/** Reads the private `apiKey` field the OpenRouterAgent was actually constructed with. */
private fun OpenRouterAgent.actualApiKey(): String {
    val field: Field = OpenRouterAgent::class.java.getDeclaredField("apiKey")
    field.isAccessible = true
    return field.get(this) as String
}

/** Reads the `systemPrompt` field the OpenRouterAgent was actually constructed with — it now
 * lives in [BaseAgent], hence [declaredFieldRecursive]. */
private fun OpenRouterAgent.actualSystemPrompt(): String =
    declaredFieldRecursive("systemPrompt").get(this) as String

/** Reads the private `modelName` field the GeminiAgent was actually constructed with. */
private fun GeminiAgent.actualModel(): String {
    val field: Field = GeminiAgent::class.java.getDeclaredField("modelName")
    field.isAccessible = true
    return field.get(this) as String
}

/** Reads the private `model` field the OpenRouterAgent was actually constructed with. */
private fun OpenRouterAgent.actualModel(): String {
    val field: Field = OpenRouterAgent::class.java.getDeclaredField("model")
    field.isAccessible = true
    return field.get(this) as String
}

/** Reads the private `primary`/`secondary` agents out of a FallbackAgent. */
private fun FallbackAgent.primaryAgent(): Any {
    val field: Field = FallbackAgent::class.java.getDeclaredField("primary")
    field.isAccessible = true
    return field.get(this)!!
}

private fun FallbackAgent.secondaryAgent(): Any {
    val field: Field = FallbackAgent::class.java.getDeclaredField("secondary")
    field.isAccessible = true
    return field.get(this)!!
}

class AgentFactoryImplTest {

    private val session = mockk<AgentSession>(relaxed = true)
    private val registry = mockk<AgentRegistry>(relaxed = true)
    private val featureFlagService = mockk<FeatureFlagService>()
    private val remoteVariableService = mockk<RemoteVariableService>()
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
    private val geminiModelFactory = mockk<GeminiModelFactory>(relaxed = true)
    private val openRouterHttpClientFactory = mockk<OpenRouterHttpClientFactory>(relaxed = true)
    private val factory = AgentFactoryImpl(
        session,
        registry,
        featureFlagService,
        remoteVariableService,
        crashReporter,
        subscriptionRepository,
        geminiModelFactory,
        openRouterHttpClientFactory
    )

    @Test
    fun `create returns a FallbackAgent when no explicit provider is chosen and both are enabled`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = AiProvider.AUTO))

        assertTrue(agent is FallbackAgent)
    }

    @Test
    fun `create wraps the explicitly selected gemini provider in a FallbackAgent, with openrouter as backup`() {
        // Both enabled: an explicit choice must still retry the other provider on a transport
        // failure (sdd-006, B-08) — only when the explicitly chosen one is the only one available
        // (see "falls back ... when the selected one is disabled" below) is it returned bare.
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = AiProvider.GEMINI)) as FallbackAgent

        assertTrue(agent.primaryAgent() is GeminiAgent)
        assertTrue(agent.secondaryAgent() is OpenRouterAgent)
    }

    @Test
    fun `create wraps the explicitly selected openrouter provider in a FallbackAgent, with gemini as backup`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = AiProvider.OPENROUTER)) as FallbackAgent

        assertTrue(agent.primaryAgent() is OpenRouterAgent)
        assertTrue(agent.secondaryAgent() is GeminiAgent)
    }

    @Test
    fun `create falls back to the other provider when the selected one is disabled`() {
        featureFlagService.stubFlags(geminiEnabled = false, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = AiProvider.GEMINI))

        assertTrue(agent is OpenRouterAgent)
    }

    @Test
    fun `create returns NoProviderAvailableAgent when both providers are disabled`() {
        featureFlagService.stubFlags(geminiEnabled = false, openRouterEnabled = false)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = AiProvider.AUTO))

        assertTrue(agent is NoProviderAvailableAgent)
    }

    @Test
    fun `create returns the single enabled provider when the other is disabled in auto mode`() {
        featureFlagService.stubFlags(geminiEnabled = false, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(AppSettings(aiProvider = AiProvider.AUTO))

        assertTrue(agent is OpenRouterAgent)
    }

    @Test
    fun `create builds GeminiAgent with the remote key when Remote Config returns a valid value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey("remote-rotated-key")
        remoteVariableService.stubRemoteOpenRouterKey(BuildConfig.OPEN_ROUTER_API_KEY)
        remoteVariableService.stubRemoteSystemPrompt()

        val agent = (factory.create(AppSettings(aiProvider = AiProvider.GEMINI)) as FallbackAgent)
            .primaryAgent() as GeminiAgent

        assertEquals("remote-rotated-key", agent.actualApiKey())
    }

    @Test
    fun `create builds GeminiAgent with the BuildConfig key when Remote Config has no value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        // Mirrors FirebaseRemoteConfigService's real fallback contract: an unfetched/unactivated
        // key returns the caller-supplied default.
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = (factory.create(AppSettings(aiProvider = AiProvider.GEMINI)) as FallbackAgent)
            .primaryAgent() as GeminiAgent

        assertEquals(BuildConfig.GEMINI_API_KEY, agent.actualApiKey())
    }

    @Test
    fun `create falls back to the BuildConfig key when Remote Config returns a blank value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey("   ")
        remoteVariableService.stubRemoteOpenRouterKey(BuildConfig.OPEN_ROUTER_API_KEY)
        remoteVariableService.stubRemoteSystemPrompt()

        val agent = (factory.create(AppSettings(aiProvider = AiProvider.GEMINI)) as FallbackAgent)
            .primaryAgent() as GeminiAgent

        assertEquals(BuildConfig.GEMINI_API_KEY, agent.actualApiKey())
    }

    @Test
    fun `create builds OpenRouterAgent with the remote key when Remote Config returns a valid value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey(BuildConfig.GEMINI_API_KEY)
        remoteVariableService.stubRemoteOpenRouterKey("remote-rotated-openrouter-key")
        remoteVariableService.stubRemoteSystemPrompt()

        val agent = (factory.create(AppSettings(aiProvider = AiProvider.OPENROUTER)) as FallbackAgent)
            .primaryAgent() as OpenRouterAgent

        assertEquals("remote-rotated-openrouter-key", agent.actualApiKey())
    }

    @Test
    fun `create falls back to the BuildConfig key when Remote Config returns a blank OpenRouter value`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey(BuildConfig.GEMINI_API_KEY)
        remoteVariableService.stubRemoteOpenRouterKey("   ")
        remoteVariableService.stubRemoteSystemPrompt()

        val agent = (factory.create(AppSettings(aiProvider = AiProvider.OPENROUTER)) as FallbackAgent)
            .primaryAgent() as OpenRouterAgent

        assertEquals(BuildConfig.OPEN_ROUTER_API_KEY, agent.actualApiKey())
    }

    @Test
    fun `create passes the user-picked model to GeminiAgent when gemini is selected`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = (
            factory.create(AppSettings(aiProvider = AiProvider.GEMINI, aiModel = "gemini-1.5-pro")) as FallbackAgent
            ).primaryAgent() as GeminiAgent

        assertEquals("gemini-1.5-pro", agent.actualModel())
    }

    @Test
    fun `create passes the user-picked model to OpenRouterAgent when openrouter is selected`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = (
            factory.create(
                AppSettings(aiProvider = AiProvider.OPENROUTER, aiModel = "anthropic/claude-3-haiku")
            ) as FallbackAgent
            ).primaryAgent() as OpenRouterAgent

        assertEquals("anthropic/claude-3-haiku", agent.actualModel())
    }

    @Test
    fun `create keeps BuildConfig models in auto mode, since no single provider owns the picked model`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = factory.create(
            AppSettings(aiProvider = AiProvider.AUTO, aiModel = "anthropic/claude-3-haiku")
        ) as FallbackAgent

        assertEquals(BuildConfig.OPEN_ROUTER_MODEL, (agent.primaryAgent() as OpenRouterAgent).actualModel())
        assertEquals(BuildConfig.GEMINI_AGENT, (agent.secondaryAgent() as GeminiAgent).actualModel())
    }

    @Test
    fun `create uses the compiled-in prompt when Remote Config has no override`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubDefaultRemoteKeys()

        val agent = (factory.create(AppSettings(aiProvider = AiProvider.OPENROUTER)) as FallbackAgent)
            .primaryAgent() as OpenRouterAgent

        assertEquals(SystemPrompts.CESTOU_ASSISTANT, agent.actualSystemPrompt())
    }

    @Test
    fun `create uses the Remote Config prompt override when one is set`() {
        featureFlagService.stubFlags(geminiEnabled = true, openRouterEnabled = true)
        remoteVariableService.stubRemoteKey(BuildConfig.GEMINI_API_KEY)
        remoteVariableService.stubRemoteOpenRouterKey(BuildConfig.OPEN_ROUTER_API_KEY)
        remoteVariableService.stubRemoteSystemPrompt("prompt atualizado remotamente")

        val agent = (factory.create(AppSettings(aiProvider = AiProvider.OPENROUTER)) as FallbackAgent)
            .primaryAgent() as OpenRouterAgent

        assertEquals("prompt atualizado remotamente", agent.actualSystemPrompt())
    }
}
