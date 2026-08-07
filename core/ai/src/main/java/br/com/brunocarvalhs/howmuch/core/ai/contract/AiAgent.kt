package br.com.brunocarvalhs.howmuch.core.ai.contract

import kotlinx.coroutines.flow.Flow

/**
 * Interface principal para interação com o Agente de IA.
 */
interface AiAgent {
    suspend fun sendMessage(prompt: String, context: AiAgentContext): Flow<String>
}
