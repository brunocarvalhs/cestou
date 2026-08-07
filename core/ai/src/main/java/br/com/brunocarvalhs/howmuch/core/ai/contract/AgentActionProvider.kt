package br.com.brunocarvalhs.howmuch.core.ai.contract

/**
 * Interface que marca um UseCase ou serviço como capaz de fornecer uma ação para a IA.
 */
interface AgentActionProvider {
    fun toAgentAction(): AgentAction<*>
}
