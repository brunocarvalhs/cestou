package br.com.brunocarvalhs.howmuch.core.ai.registry

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentActionProvider

/**
 * Registro central de todas as ações (ferramentas) que o Agente de IA pode executar.
 */
object AgentRegistry {

    private val actions = mutableMapOf<String, AgentAction<*>>()

    fun register(action: AgentAction<*>) {
        actions[action.id] = action
    }

    fun registerProvider(vararg providers: AgentActionProvider) {
        providers.forEach { provider ->
            val action = provider.toAgentAction()
            actions[action.id] = action
        }
    }

    fun find(id: String): AgentAction<*>? = actions[id]

    fun getAll(): List<AgentAction<*>> = actions.values.toList()

    fun clear() {
        actions.clear()
    }
}
