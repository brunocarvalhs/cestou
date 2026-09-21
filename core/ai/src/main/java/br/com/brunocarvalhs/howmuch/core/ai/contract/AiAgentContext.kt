package br.com.brunocarvalhs.howmuch.core.ai.contract

/**
 * Interface que permite que estados da UI ou DTOs forneçam contexto dinâmico para a IA.
 * Implemente esta interface em seus UiStates para que o Agente saiba o que o usuário está vendo.
 */
interface AiAgentContext {
    fun toMetadata(): Map<String, Any?>
}
