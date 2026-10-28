package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

/**
 * Converte o [Result] de uma [br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction] no texto
 * devolvido ao modelo como resultado da tool call. Compartilhado por [GeminiAiAgent] e
 * [OpenRouterAiAgent] porque os dois tratam o mesmo contrato (`AgentAction.execute(): Result<R>`).
 *
 * Antes desta função, `getOrNull()?.toString() ?: "Sucesso"` informava `Result.failure` como
 * sucesso e `Result.success(Unit)` como o literal `"kotlin.Unit"` — o modelo alucinava sobre o
 * que de fato aconteceu (sdd-006, B-09).
 */
internal fun Result<*>.toToolFeedback(): String = fold(
    onSuccess = { value -> if (value == null || value == Unit) "OK" else value.toString() },
    onFailure = { error -> "Erro: ${error.message}" }
)
