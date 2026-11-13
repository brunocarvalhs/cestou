package br.com.brunocarvalhs.howmuch.wear

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.ai.model.REQUIRE_WRITE_CONFIRMATION_KEY
import br.com.brunocarvalhs.howmuch.core.common.wearable.AiFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableAiLink
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.exception.AiProviderUnavailableException
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.orchestrator.AgentOrchestrator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone side of the watch's AI quick entry. The watch sends a prompt; this runs the agent
 * here, with the phone's own key, and sends the answer back. The key never leaves the phone.
 *
 * Each watch prompt starts from an empty history: it is a one-shot entry, not a chat. Approvals of
 * irreversible actions are forwarded to the watch as [WearAiMessage.ConfirmRequest] and resolved
 * through the same [AiSession] approval the phone's chat uses.
 */
@Singleton
class WearAiRelay @Inject constructor(
    private val orchestrator: AgentOrchestrator,
    private val session: AiSession,
    private val wearableAi: WearableAiLink
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Starts answering [prompt] for the watch node that asked. Never throws into the listener. */
    fun answer(watchNodeId: String, requestId: String, prompt: String) {
        scope.launch { answerNow(watchNodeId, requestId, prompt) }
    }

    /** Resolves the open approval, but only when it is the one the watch answered. */
    fun confirm(actionId: String, approved: Boolean) {
        val pending = session.pendingApproval.value ?: return
        if (pending.actionId != actionId) {
            Timber.tag("WearAi").w("Ignoring an approval for an action that is not pending")
            return
        }
        session.resolveApproval(if (approved) ApprovalDecision.APPROVED else ApprovalDecision.REJECTED)
    }

    private suspend fun answerNow(watchNodeId: String, requestId: String, prompt: String) = coroutineScope {
        if (session.userId == null) {
            wearableAi.sendAiMessage(watchNodeId, WearAiMessage.Failure(requestId, AiFailure.ERROR))
            return@coroutineScope
        }
        orchestrator.clearHistory()
        val forwardApprovals = launch {
            session.pendingApproval.filterNotNull().collect { request ->
                wearableAi.sendAiMessage(
                    watchNodeId,
                    WearAiMessage.ConfirmRequest(requestId, request.actionId, request.summary.orEmpty())
                )
            }
        }
        val message = runCatching { collectAnswer(prompt) }
            .fold(
                onSuccess = { answer -> WearAiMessage.Reply(requestId, answer) },
                onFailure = { error -> failureFor(requestId, error) }
            )
        forwardApprovals.cancel()
        wearableAi.sendAiMessage(watchNodeId, message)
    }

    /**
     * Maps an agent failure to the message the watch shows. Cancellation is rethrown, so a stopped
     * scope never turns into a failure reply. Only the error class is logged, never its message,
     * which can carry the user's text.
     */
    private fun failureFor(requestId: String, error: Throwable): WearAiMessage {
        if (error is CancellationException) throw error
        if (error is AiProviderUnavailableException) return WearAiMessage.Failure(requestId, AiFailure.NO_PROVIDER)
        Timber.tag("WearAi").w("The agent failed on a watch prompt: %s", error::class.java.simpleName)
        return WearAiMessage.Failure(requestId, AiFailure.ERROR)
    }

    private suspend fun collectAnswer(prompt: String): String {
        val answer = StringBuilder()
        orchestrator.chat(prompt, WatchAiContext).collect { chunk -> answer.append(chunk) }
        return answer.toString()
    }
}

/**
 * The watch prompt carries no screen context: the phone's UI state must not leak into it. The one
 * metadata entry asks for approval of every write action, because a voice or typed prompt from the
 * watch can be misheard.
 */
private object WatchAiContext : AgentContext {
    override fun toMetadata(): Map<String, Any?> = mapOf(REQUIRE_WRITE_CONFIRMATION_KEY to true)
}
