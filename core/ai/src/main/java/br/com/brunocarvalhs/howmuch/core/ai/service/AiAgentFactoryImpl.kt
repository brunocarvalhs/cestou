package br.com.brunocarvalhs.howmuch.core.ai.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import javax.inject.Inject

class AiAgentFactoryImpl @Inject constructor(
    private val session: AiAgentSession
) : AiAgentFactory {
    override fun create(settings: AppSettings): AiAgent {
        return FallbackAiAgent(
            primary = OpenRouterAiAgent(
                session = session,
                modelName = settings.aiModel,
                customPrompt = settings.customPrompt,
                temperature = settings.creativityLevel
            ),
            secondary = GeminiAiAgent(session = session)
        )
    }
}
