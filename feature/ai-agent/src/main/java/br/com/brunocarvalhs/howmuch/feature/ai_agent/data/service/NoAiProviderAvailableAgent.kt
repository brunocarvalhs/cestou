package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.exception.AiProviderUnavailableException
import kotlinx.coroutines.flow.flow

/**
 * Usado quando [AiAgentFactoryImpl] desliga todos os providers via feature flag remota.
 */
internal object NoAiProviderAvailableAgent : AiAgent {

    override suspend fun sendMessage(prompt: String, context: AiAgentContext) = flow<String> {
        throw AiProviderUnavailableException()
    }
}
