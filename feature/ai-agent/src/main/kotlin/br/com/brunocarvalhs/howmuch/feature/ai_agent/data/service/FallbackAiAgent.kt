package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import timber.log.Timber

/**
 * Agente de resiliência que tenta usar um agente primário e, em caso de erro, recorre ao secundário.
 * Se o secundário também falhar, emite uma mensagem amigável em vez de propagar a exceção —
 * este é o único lugar da cadeia onde o usuário deve ver um "desculpe, algo deu errado".
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
                emitAll(
                    secondary.sendMessage(prompt, context)
                        .catch { secondaryError ->
                            Timber.e(secondaryError, "Agente secundário também falhou")
                            emit("Desculpe, tive um problema ao processar sua solicitação. Tente novamente em instantes.")
                        }
                )
            }
    }
}
