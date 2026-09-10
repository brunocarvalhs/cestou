package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
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
    private val remoteVariableService: RemoteVariableService
) : AiAgentFactory {

    override fun create(settings: AppSettings): AiAgent {
        // Re-read on every call (unlike the @Singleton `by lazy` repositories in
        // feature/products), so a rotated key here takes effect immediately, without an
        // app restart.
        val geminiApiKey = resolveApiKey(RemoteVariableKeys.GEMINI_API_KEY, BuildConfig.GEMINI_API_KEY)
        val gemini = GeminiAiAgent(session, registry, apiKey = geminiApiKey)
            .takeIf { featureFlagService.isEnabled(FeatureFlagKeys.AI_GEMINI_ENABLED, default = true) }
        val openRouterApiKey =
            resolveApiKey(RemoteVariableKeys.OPEN_ROUTER_API_KEY, BuildConfig.OPEN_ROUTER_API_KEY)
        val openRouter = OpenRouterAiAgent(session, registry, apiKey = openRouterApiKey)
            .takeIf { featureFlagService.isEnabled(FeatureFlagKeys.AI_OPENROUTER_ENABLED, default = true) }

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
    private fun resolveApiKey(key: String, default: String): String =
        remoteVariableService.getString(key = key, default = default)
            .takeIf { it.isNotBlank() } ?: default
}
