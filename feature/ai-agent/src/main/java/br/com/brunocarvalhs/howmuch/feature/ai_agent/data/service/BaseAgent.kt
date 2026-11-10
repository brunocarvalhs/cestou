package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.Agent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalRequest
import br.com.brunocarvalhs.howmuch.core.ai.model.requiresApproval
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
        } else if (action != null && requiresApproval(action, meta) && !isApprovedByUser(action, arguments)) {
            DECLINED_MESSAGE
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
     * Ação irreversível: abre o card de aprovação e **espera** a decisão dentro do próprio dispatch.
     * Aprovada, segue para a execução normal deste mesmo ponto (Pro, telemetria, erro); recusada,
     * sem resposta a tempo ou com o chat fechado, nada é executado. Só o `action_id` vai para a
     * telemetria, nunca o resumo nem os argumentos.
     */
    private suspend fun isApprovedByUser(action: AgentAction<*>, arguments: Map<String, Any?>): Boolean {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_TOOL_CONFIRMATION_REQUESTED,
            mapOf(AnalyticsParams.ACTION_ID to action.id)
        )
        val decision = session.awaitApproval(ApprovalRequest(action.id, summaryOf(action, arguments)))
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_TOOL_CONFIRMATION_RESOLVED,
            mapOf(
                AnalyticsParams.ACTION_ID to action.id,
                AnalyticsParams.DECISION to decision.name.lowercase()
            )
        )
        return decision.isApproved
    }

    /** O resumo é só um apoio ao card: se falhar, o card sai com o título genérico. */
    private suspend fun summaryOf(action: AgentAction<*>, arguments: Map<String, Any?>): String? = try {
        action.confirmationSummary(arguments)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(logTag).w(e, "Sem resumo para o card de '%s'", action.id)
        null
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
        const val DECLINED_MESSAGE =
            "NÃO EXECUTADO: o usuário recusou esta ação, ou não respondeu a tempo. Nada foi alterado. " +
                "Diga isso em uma frase simples e pergunte se ele quer fazer outra coisa."
    }
}
