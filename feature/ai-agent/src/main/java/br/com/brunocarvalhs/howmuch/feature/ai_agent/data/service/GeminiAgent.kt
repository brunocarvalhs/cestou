package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionCallPart
import com.google.ai.client.generativeai.type.FunctionResponsePart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import timber.log.Timber

/**
 * Implementação do Agente utilizando Google Gemini AI SDK.
 */
internal class GeminiAgent(
    dependencies: AgentDependencies,
    private val modelFactory: GeminiModelFactory,
    private val modelName: String = BuildConfig.GEMINI_AGENT,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) : BaseAgent(dependencies) {

    override val providerTag: String = "gemini"
    override val modelLabel: String get() = modelName

    private val generativeModel: GenerativeModel by lazy {
        modelFactory.create(
            modelName = modelName,
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            tools = registry.getAll()
        )
    }

    override suspend fun sendMessage(
        prompt: String,
        context: AgentContext
    ): Flow<String> = flow {
        val chat = generativeModel.startChat(history = session.history)
        val meta = context.toMetadata()
        // Zera no início da tentativa: FallbackAgent lê este sinal só depois que esta chamada
        // falha, então precisa refletir só o que aconteceu *nesta* tentativa (sdd-012).
        session.toolExecutedThisTurn = false

        try {
            // systemInstruction is bound once when generativeModel is built (see below), so a
            // per-request value like the current shoppingId can't go there — it rides along on
            // the turn's own prompt instead, which the model reads exactly the same way.
            var response = chat.sendMessage(prompt + SystemPrompts.contextSuffix(meta))
            var rounds = 0

            // Loop para processar Function Calling
            while (response.functionCalls.isNotEmpty()) {
                if (rounds >= MAX_TOOL_ROUNDS) {
                    emit(TOOL_ROUNDS_EXCEEDED_MESSAGE)
                    session.history.clear()
                    session.history.addAll(chat.history)
                    session.history.trimToRecentHistory()
                    return@flow
                }
                rounds++

                val toolResponses = response.functionCalls.map { functionCall ->
                    toFunctionResponsePart(functionCall, meta)
                }

                // Cria o conteúdo da resposta das ferramentas e envia de volta
                val toolContent = Content(role = "tool", parts = toolResponses)
                response = chat.sendMessage(toolContent)
            }

            response.text?.trim()?.let {
                emit(it)
                // Atualiza o histórico da sessão com o estado atual do chat
                session.history.clear()
                session.history.addAll(chat.history)
                session.history.trimToRecentHistory()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.tag(TAG).e(e, "Erro na comunicação com Gemini (model=%s)", modelName)
            crashReporter.recordException(
                e,
                extras = mapOf(
                    "provider" to providerTag,
                    "model" to modelName
                )
            )
            // Relança em vez de emitir um texto amigável: só o ponto mais externo da cadeia de
            // agentes (FallbackAgent, se houver um secundário; senão AiChatViewModel) decide o
            // que o usuário vê. Antes, emitir aqui escondia a falha de FallbackAgent.catch,
            // que nunca disparava (sdd-006, B-08) — ver FallbackAgentTest, que já provava que
            // o fallback em si funciona.
            throw e
        }
    }

    /**
     * Adapta a function call do SDK do Gemini para o formato de [BaseAgent.executeAction]
     * (AD-010) e empacota o resultado de volta no formato que o SDK espera de retorno. Não
     * executa nada por si só — quem executa é [BaseAgent.executeAction]. `internal` (não
     * `private`) para continuar diretamente testável, como já era antes desta extração
     * (sdd-026, A-03) — ver [GeminiAgentGatingTest].
     */
    internal suspend fun toFunctionResponsePart(
        functionCall: FunctionCallPart,
        meta: Map<String, Any?>
    ): FunctionResponsePart {
        val result = executeAction(functionCall.name, functionCall.args, meta)
        return FunctionResponsePart(
            name = functionCall.name,
            response = JSONObject().apply { put("result", result) }
        )
    }

    companion object {
        private const val TAG = "GeminiAgent"
    }
}
