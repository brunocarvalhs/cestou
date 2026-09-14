package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteVariableKeys
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AiAgentSession
import javax.inject.Inject

/**
 * Factory para criação de instâncias de AiAgent baseadas nas configurações do usuário.
 *
 * Cada provider pode ser desligado remotamente (por versão, se necessário) via
 * [FeatureFlagKeys.AI_GEMINI_ENABLED]/[FeatureFlagKeys.AI_OPENROUTER_ENABLED] — útil para
 * cortar um provider com bug em produção sem publicar uma nova versão do app.
 */
internal class AiAgentFactoryImpl @Inject constructor(
    private val session: AiAgentSession,
    private val registry: AgentRegistry,
    private val featureFlagService: FeatureFlagService,
    private val remoteVariableService: RemoteVariableService,
    private val crashReporter: CrashReporter
) : AiAgentFactory {

    override fun create(settings: AppSettings): AiAgent {
        // Re-read on every call (unlike the @Singleton `by lazy` repositories in
        // feature/products), so a rotated key here takes effect immediately, without an
        // app restart.
        val geminiApiKey = resolveRemoteString(RemoteVariableKeys.GEMINI_API_KEY, BuildConfig.GEMINI_API_KEY)
        // The behavior prompt is remote so a missed guardrail or scope issue can be tightened
        // instantly for everyone, without waiting on a Play Store rollout.
        val systemPrompt = resolveRemoteString(RemoteVariableKeys.AI_SYSTEM_PROMPT, SystemPrompts.CESTOU_ASSISTANT)
        // settings.aiModel is the single model the user picked in AiSettingsScreen; it only
        // applies to whichever provider they actually selected (settings.aiProvider) — the
        // other provider here only exists as a fallback and keeps its BuildConfig default,
        // since the picked model id isn't guaranteed to be valid for it.
        val geminiModel = settings.aiModel
            .takeIf { settings.aiProvider == "gemini" && it.isNotBlank() }
            ?: BuildConfig.GEMINI_AGENT
        val gemini = GeminiAiAgent(
            session,
            registry,
            modelName = geminiModel,
            apiKey = geminiApiKey,
            crashReporter = crashReporter,
            systemPrompt = systemPrompt
        ).takeIf { featureFlagService.isEnabled(FeatureFlagKeys.AI_GEMINI_ENABLED, default = true) }
        val openRouterApiKey =
            resolveRemoteString(RemoteVariableKeys.OPEN_ROUTER_API_KEY, BuildConfig.OPEN_ROUTER_API_KEY)
        val openRouterModel = settings.aiModel
            .takeIf { settings.aiProvider == "openrouter" && it.isNotBlank() }
            ?: BuildConfig.OPEN_ROUTER_MODEL
        val openRouter = OpenRouterAiAgent(
            session,
            registry,
            model = openRouterModel,
            apiKey = openRouterApiKey,
            crashReporter = crashReporter,
            systemPrompt = systemPrompt
        ).takeIf { featureFlagService.isEnabled(FeatureFlagKeys.AI_OPENROUTER_ENABLED, default = true) }

        return when (settings.aiProvider) {
            "gemini" -> gemini ?: openRouter ?: NoAiProviderAvailableAgent
            "openrouter" -> openRouter ?: gemini ?: NoAiProviderAvailableAgent
            else -> when {
                openRouter != null && gemini != null -> FallbackAiAgent(openRouter, gemini)
                openRouter != null -> openRouter
                gemini != null -> gemini
                else -> NoAiProviderAvailableAgent
            }
        }
    }

    /** Reads [key] from Remote Config, guarding against a blank remote value (see AD-008)
     * by falling back to the compiled [default] verbatim, just as an unfetched/unactivated
     * console value would.
     */
    private fun resolveRemoteString(key: String, default: String): String =
        remoteVariableService.getString(key = key, default = default)
            .takeIf { it.isNotBlank() } ?: default
}
