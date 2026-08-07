package br.com.brunocarvalhs.howmuch.core.ai.base

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentActionProvider
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import timber.log.Timber


/**
 * Classe base para UseCases que funcionam como ferramentas (actions) para o Agente de IA.
 * Extrai automaticamente metadados das anotações @AiAgentAction e @AiAgentParameter.
 */
abstract class AgentActionUseCase<R> : AgentAction<R>, AgentActionProvider {

    private val actionDef: AiAgentAction by lazy {
        this::class.annotations.filterIsInstance<AiAgentAction>().firstOrNull()
            ?: throw IllegalStateException("A classe ${this::class.simpleName} deve ser anotada com @AiAgentAction")
    }

    override val id: String get() = actionDef.id
    override val description: String get() = actionDef.description

    override val parameters: List<AiAgentParameter> by lazy {
        this::class.annotations
            .filterIsInstance<br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter>()
            .map {
                AiAgentParameter(it.name, it.type, it.description, it.isRequired)
            }
    }

    init {
        @Suppress("LeakingThis")
        AgentRegistry.register(this).also {
            Timber.d("✅ Ferramenta de IA registrada: $id - $description")
        }
    }

    override fun toAgentAction(): AgentAction<*> = this
}
