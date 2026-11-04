package br.com.brunocarvalhs.howmuch.core.ai.contract

import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentParameter

/**
 * Contrato base para qualquer ação que a IA possa executar.
 */
interface AgentAction<R> {
    val id: String
    val description: String
    val parameters: List<AiAgentParameter> get() = emptyList()

    /**
     * Whether this action is gated behind the Pro subscription (AD-010). Checked once, at the
     * dispatch chokepoint (`GeminiAgent`/`OpenRouterAgent`), never per use case. Defaults to
     * `false` - no real action flips this yet, that is a pending commercial decision.
     */
    val requiresPro: Boolean get() = false

    /**
     * Ação irreversível (apagar listas ou itens). O dispatch chokepoint só a executa depois de o
     * usuário aprovar no card; a IA sozinha não consegue se autorizar.
     */
    val requiresConfirmation: Boolean get() = false

    /**
     * Resumo do que a ação vai afetar, mostrado no card de aprovação (ex. o título da lista);
     * `null` usa só o título genérico da ação. Nunca deve conter ids.
     */
    suspend fun confirmationSummary(arguments: Map<String, Any?>): String? = null

    suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?> = emptyMap()
    ): Result<R>
}
