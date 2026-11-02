package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.Agent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.exception.AiProviderUnavailableException
import kotlinx.coroutines.flow.flow

/**
 * Usado quando [AgentFactoryImpl] desliga todos os providers via feature flag remota.
 */
internal object NoProviderAvailableAgent : Agent {

    override suspend fun sendMessage(prompt: String, context: AgentContext) = flow<String> {
        throw AiProviderUnavailableException()
    }
}
