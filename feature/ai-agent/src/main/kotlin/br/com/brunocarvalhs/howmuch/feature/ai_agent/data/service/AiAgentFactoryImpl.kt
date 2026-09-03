package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
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
    private val featureFlagService: FeatureFlagService
) : AiAgentFactory {

    override fun create(settings: AppSettings): AiAgent {
        val gemini = GeminiAiAgent(session, registry)
            .takeIf { featureFlagService.isEnabled(FeatureFlagKeys.AI_GEMINI_ENABLED, default = true) }
        val openRouter = OpenRouterAiAgent(session, registry)
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
}
