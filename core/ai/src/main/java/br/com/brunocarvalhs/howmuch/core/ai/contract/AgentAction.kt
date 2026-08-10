package br.com.brunocarvalhs.howmuch.core.ai.contract

import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession

/**
 * Contrato base para qualquer ação que a IA possa executar.
 */
interface AgentAction<R> {
    val id: String
    val description: String
    val parameters: List<AiAgentParameter> get() = emptyList()

    suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?> = emptyMap()
    ): Result<R>
}
