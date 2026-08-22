package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AiAgentSession
import javax.inject.Inject

/**
 * Factory para criação de instâncias de AiAgent baseadas nas configurações do usuário.
 */
internal class AiAgentFactoryImpl @Inject constructor(
    private val session: AiAgentSession,
    private val registry: AgentRegistry
) : AiAgentFactory {

    override fun create(settings: AppSettings): AiAgent {
        val gemini = GeminiAiAgent(session, registry)
        val openRouter = OpenRouterAiAgent(session, registry)

        return when (settings.aiProvider) {
            "gemini" -> gemini
            "openrouter" -> openRouter
            else -> FallbackAiAgent(gemini, openRouter)
        }
    }
}
