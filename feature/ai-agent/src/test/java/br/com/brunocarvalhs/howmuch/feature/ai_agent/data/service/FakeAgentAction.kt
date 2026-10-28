package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession

/** Minimal [AgentAction] test double for exercising the Pro gate at the dispatch chokepoint. */
internal class FakeAgentAction(
    override val id: String = "fake_action",
    override val requiresPro: Boolean = false,
    private val result: Result<Any?> = Result.success("ok"),
    private val throwable: Throwable? = null
) : AgentAction<Any?> {

    override val description: String = "fake action for tests"
    var executed: Boolean = false

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<Any?> {
        executed = true
        throwable?.let { throw it }
        return result
    }
}
