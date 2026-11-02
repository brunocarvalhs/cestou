package br.com.brunocarvalhs.howmuch.core.ai.registry

import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro central de todas as ações (ferramentas) que o Agente de IA pode executar. Construído
 * uma única vez a partir do [Set] de [AgentActionUseCase] que o Hilt já agrega via `@IntoSet` em
 * cada feature (`ShoppingAgentModule`, `ProductAgentModule`) — não há mais `register`/`clear`
 * (sdd-027, A-01): o `Set` do Hilt já é a fonte de verdade completa no momento da construção.
 */
@Singleton
class AgentRegistry @Inject constructor(
    actions: Set<@JvmSuppressWildcards AgentActionUseCase<*>>
) {

    private val byId: Map<String, AgentActionUseCase<*>> = actions.associateBy { it.id }

    fun find(id: String): AgentAction<*>? = byId[id]

    fun getAll(): List<AgentAction<*>> = byId.values.toList()
}
