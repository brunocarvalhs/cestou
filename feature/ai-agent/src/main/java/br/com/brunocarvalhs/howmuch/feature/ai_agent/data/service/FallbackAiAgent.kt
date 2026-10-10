package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/**
 * Agente de resiliência que tenta usar um agente primário e, em caso de erro, recorre ao secundário.
 */
internal class FallbackAiAgent(
    private val primary: AiAgent,
    private val secondary: AiAgent
) : AiAgent {

    override suspend fun sendMessage(
        prompt: String,
        context: AiAgentContext
    ): Flow<String> {
        return primary.sendMessage(prompt, context)
            .catch { error ->
                Timber.w(error, "Agente primário falhou, tentando secundário...")
                secondary.sendMessage(prompt, context).collect { emit(it) }
            }
    }
}
