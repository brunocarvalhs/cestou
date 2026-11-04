package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.Agent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
    private val analyticsTracker: AnalyticsTracker = dependencies.analyticsTracker
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
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_TOOL_BLOCKED_PRO,
                mapOf(AnalyticsParams.ACTION_ID to action.id)
            )
            "Essa ação é exclusiva do plano Pro."
        } else if (action != null && action.requiresConfirmation && !isApprovedByUser(action.id, arguments)) {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_TOOL_CONFIRMATION_REQUESTED,
                mapOf(AnalyticsParams.ACTION_ID to action.id)
            )
            CONFIRMATION_REQUIRED_MESSAGE
        } else if (action != null) {
            try {
                // Marcado antes de executar, não depois: mesmo uma execução que termina em
                // exceção pode ter mutação parcial (sdd-012) — conservador de propósito.
                session.toolExecutedThisTurn = true
                session.toolCallsThisTurn++
                session.setActiveAction(action.id)
                val startedAt = TimeSource.Monotonic.markNow()
                val result = action.execute(
                    arguments = arguments,
                    session = session,
                    metadata = meta
                )
                trackTool(action.id, success = result.isSuccess, startedAt)
                result.toToolFeedback()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                trackTool(action.id, success = false, startedAt = null)
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
            } finally {
                session.setActiveAction(null)
            }
        } else {
            Timber.tag(logTag).w("Ação '%s' não encontrada no registry", actionName)
            "Erro: Ação '$actionName' não encontrada"
        }
    }

    /**
     * A primeira chamada só registra o pedido; a execução exige repetir a mesma ação com os
     * mesmos argumentos num turno posterior, ou seja, depois de o usuário responder.
     */
    private fun isApprovedByUser(actionId: String, arguments: Map<String, Any?>): Boolean {
        val key = actionId + arguments.entries.sortedBy { it.key }.joinToString(prefix = "(", postfix = ")")
        if (session.consumeConfirmation(key)) return true
        session.requestConfirmation(key)
        return false
    }

    /**
     * [startedAt] é nulo na falha porque a exceção pode vir antes de a ação começar a medir;
     * só o `action_id` (nome fixo da ação registrada) é enviado, nunca os argumentos.
     */
    private fun trackTool(actionId: String, success: Boolean, startedAt: TimeMark?) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_TOOL_EXECUTED,
            buildMap {
                put(AnalyticsParams.ACTION_ID, actionId)
                put(AnalyticsParams.SUCCESS, success)
                put(AnalyticsParams.PROVIDER, providerTag)
                startedAt?.let { put(AnalyticsParams.DURATION_MS, it.elapsedNow().inWholeMilliseconds) }
            }
        )
    }

    /** Point-in-time read of the current entitlement (AD-010); the gate only needs "right now". */
    protected suspend fun isPro(): Boolean =
        subscriptionRepository.observeStatus().first() == SubscriptionStatus.PRO

    private companion object {
        /** Devolvido ao modelo no lugar do resultado: nada foi executado. */
        const val CONFIRMATION_REQUIRED_MESSAGE =
            "NÃO EXECUTADO: esta ação é irreversível e o usuário ainda não autorizou. Diga em " +
                "linguagem simples o que será apagado e pergunte se ele confirma. Só chame a ação " +
                "de novo, com os mesmos argumentos, depois que o usuário responder que sim."
    }
}
