package br.com.brunocarvalhs.howmuch.core.ai.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import timber.log.Timber

/**
 * Um modelo de agente que tenta usar um modelo principal e, se falhar, utiliza um modelo de fallback.
 */
class FallbackAiAgent(
    private val primary: AiAgent,
    private val secondary: AiAgent
) : AiAgent {

    override suspend fun sendMessage(prompt: String, context: AiAgentContext): Flow<String> {
        return primary.sendMessage(prompt, context).catch { e ->
            Timber.w(e, "Primary agent failed, switching to secondary")
            emitAll(secondary.sendMessage(prompt, context))
        }
    }
}
