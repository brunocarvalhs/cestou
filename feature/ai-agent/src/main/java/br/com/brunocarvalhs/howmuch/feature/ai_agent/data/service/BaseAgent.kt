package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.Agent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Base compartilhada pelos providers de IA ([GeminiAgent], [OpenRouterAgent]). Extrai o
 * dispatch chokepoint (gate Pro + execução no [AgentRegistry] + tratamento de erro) que antes era
 * duplicado quase byte a byte nas duas classes (sdd-026, A-03) — o loop de function-calling de
 * [sendMessage], cujo formato difere real entre os dois SDKs, continua em cada subclasse.
 */
internal abstract class BaseAgent(
    dependencies: AgentDependencies
) : Agent {

    protected val session: AiSession = dependencies.session
    protected val registry: AgentRegistry = dependencies.registry
    protected val crashReporter: CrashReporter = dependencies.crashReporter
    protected val systemPrompt: String = dependencies.systemPrompt
    private val subscriptionRepository: SubscriptionRepository = dependencies.subscriptionRepository

    /** `"gemini"` | `"openrouter"` — valor da chave `provider` nos extras do [crashReporter]. */
    protected abstract val providerTag: String

    /** Rótulo do modelo efetivamente usado pela subclasse, só para telemetria. */
    protected abstract val modelLabel: String

    private val logTag: String = this::class.simpleName.orEmpty()

    /**
     * Dispatch chokepoint (AD-010): resolve [actionName] no [registry], aplica o gate Pro quando
     * [br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction.requiresPro] estiver marcado, e
     * só então executa de fato a ação encontrada — é o único lugar da classe que roda
     * [br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction.execute]. `internal` (não
     * `private`) para o gate continuar diretamente testável, como já era em cada subclasse antes
     * desta extração.
     */
    internal suspend fun executeAction(
        actionName: String,
        arguments: Map<String, Any?>,
        meta: Map<String, Any?>
    ): String {
        val action = registry.find(actionName)
        return if (action != null && action.requiresPro && !isPro()) {
            "Essa ação é exclusiva do plano Pro."
        } else if (action != null) {
            try {
                // Marcado antes de executar, não depois: mesmo uma execução que termina em
                // exceção pode ter mutação parcial (sdd-012) — conservador de propósito.
                session.toolExecutedThisTurn = true
                action.execute(
                    arguments = arguments,
                    session = session,
                    metadata = meta
                ).toToolFeedback()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.tag(logTag).e(e, "Erro executando a função '%s' com args=%s", actionName, arguments)
                crashReporter.recordException(
                    e,
                    extras = mapOf(
                        "provider" to providerTag,
                        "model" to modelLabel,
                        "function" to actionName
                    )
                )
                "Erro na execução da ação: ${e.message}"
            }
        } else {
            Timber.tag(logTag).w("Ação '%s' não encontrada no registry", actionName)
            "Erro: Ação '$actionName' não encontrada"
        }
    }

    /** Point-in-time read of the current entitlement (AD-010); the gate only needs "right now". */
    protected suspend fun isPro(): Boolean =
        subscriptionRepository.observeStatus().first() == SubscriptionStatus.PRO
}
