package br.com.brunocarvalhs.howmuch.core.ai.contract

import kotlinx.coroutines.flow.Flow

/**
 * Interface principal para interação com o Agente de IA.
 */
interface Agent {
    suspend fun sendMessage(prompt: String, context: AgentContext): Flow<String>
}
