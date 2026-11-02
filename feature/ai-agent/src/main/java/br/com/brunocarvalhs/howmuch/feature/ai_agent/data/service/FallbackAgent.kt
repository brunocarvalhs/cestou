package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.Agent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/**
 * Agente de resiliência que tenta usar um agente primário e, em caso de erro, recorre ao secundário.
 */
internal class FallbackAgent(
    private val primary: Agent,
    private val secondary: Agent,
    private val session: AiSession
) : Agent {

    override suspend fun sendMessage(
        prompt: String,
        context: AgentContext
    ): Flow<String> {
        return primary.sendMessage(prompt, context)
            .catch { error ->
                // Se o primário já despachou uma tool com efeito colateral real antes de falhar
                // (ex. produto já gravado no Firestore), tentar o secundário reexecutaria o
                // pedido inteiro do zero e duplicaria essa tool (sdd-012). Relança em vez de
                // retentar — o usuário vê a mesma mensagem de erro de hoje (AiChatViewModel),
                // mas a ação não é repetida.
                if (session.toolExecutedThisTurn) {
                    throw error
                }
                Timber.w(error, "Agente primário falhou, tentando secundário...")
                secondary.sendMessage(prompt, context).collect { emit(it) }
            }
    }
}
