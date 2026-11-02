package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.ChatRequest
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.ChatResponse
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.FunctionCall
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.Message
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Implementação do Agente utilizando a API do OpenRouter.
 * Suporta múltiplos modelos (como GPT-4, Claude, etc) e Function Calling.
 */
internal class OpenRouterAiAgent(
    dependencies: AiAgentDependencies,
    private val httpClientFactory: OpenRouterHttpClientFactory,
    private val model: String = BuildConfig.OPEN_ROUTER_MODEL,
    private val apiKey: String = BuildConfig.OPEN_ROUTER_API_KEY
) : BaseAgent(dependencies) {

    override val providerTag: String = "openrouter"
    override val modelLabel: String get() = model

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client: HttpClient by lazy { httpClientFactory.create(json) }

    override suspend fun sendMessage(
        prompt: String,
        context: AiAgentContext
    ): Flow<String> = flow {
        val meta = context.toMetadata()
        // Zera no início da tentativa: FallbackAiAgent lê este sinal só depois que esta chamada
        // falha, então precisa refletir só o que aconteceu *nesta* tentativa (sdd-012).
        session.toolExecutedThisTurn = false

        try {
            val messages = buildInitialMessages(prompt, meta)
            val tools = buildOpenRouterTools(registry.getAll())

            var currentRequest = ChatRequest(
                model = model,
                messages = messages,
                tools = tools,
                maxTokens = MAX_RESPONSE_TOKENS
            )
            var response: ChatResponse = executeRequest(currentRequest)
            var rounds = 0

            // Loop para processar Function Calling
            while (response.choices.firstOrNull()?.message?.toolCalls?.isNotEmpty() == true) {
                if (rounds >= MAX_TOOL_ROUNDS) {
                    emit(TOOL_ROUNDS_EXCEEDED_MESSAGE)
                    recordTurnInHistory(prompt, TOOL_ROUNDS_EXCEEDED_MESSAGE)
                    return@flow
                }
                rounds++

                val assistantMessage = response.choices.first().message
                messages.add(assistantMessage)

                assistantMessage.toolCalls?.forEach { toolCall ->
                    val result = toToolCallContent(toolCall.function, meta)

                    messages.add(
                        Message(
                            role = "tool",
                            toolCallId = toolCall.id,
                            content = result
                        )
                    )
                }

                currentRequest = ChatRequest(
                    model = model, messages = messages, tools = tools, maxTokens = MAX_RESPONSE_TOKENS
                )
                response = executeRequest(currentRequest)
            }

            response.choices.firstOrNull()?.message?.content?.trim()?.let { text ->
                emit(text)
                recordTurnInHistory(prompt, text)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.tag(TAG).e(e, "Erro na comunicação com OpenRouter (model=%s)", model)
            crashReporter.recordException(
                e,
                extras = mapOf(
                    "provider" to providerTag,
                    "model" to model
                )
            )
            // Relança em vez de emitir um texto amigável — ver o mesmo comentário em
            // GeminiAiAgent.sendMessage (sdd-006, B-08).
            throw e
        }
    }

    /**
     * Converte o histórico e o prompt atual para o formato de mensagens do OpenRouter.
     */
    private fun buildInitialMessages(prompt: String, meta: Map<String, Any?>): MutableList<Message> {
        val messages = mutableListOf<Message>()

        // Adiciona instrução de sistema
        messages.add(Message(role = "system", content = systemPrompt))

        // Converte histórico do session (Gemini Content format) para Message format
        session.history.forEach { content ->
            val text = content.parts.filterIsInstance<TextPart>().joinToString(" ") { it.text }
            if (text.isNotEmpty()) {
                messages.add(Message(role = openRouterRoleFor(content.role), content = text))
            }
        }

        // Adiciona mensagem atual (+ contexto desta conversa, ex: shoppingId atual — colado
        // junto da pergunta em vez de enterrado no fim do system prompt, já que modelos
        // gratuitos/fracos tendem a seguir melhor uma instrução perto do que foi perguntado)
        messages.add(Message(role = "user", content = prompt + SystemPrompts.contextSuffix(meta)))
        return messages
    }

    /** Atualiza o histórico (simplificado para demonstração) com o turno concluído. */
    private fun recordTurnInHistory(prompt: String, responseText: String) {
        session.history.add(Content(role = "user", parts = listOf(TextPart(prompt))))
        session.history.add(Content(role = "model", parts = listOf(TextPart(responseText))))
        session.history.trimToRecentHistory()
    }

    private suspend fun executeRequest(request: ChatRequest): ChatResponse {
        val requestJson = json.encodeToString(ChatRequest.serializer(), request)
        Timber.tag(TAG).d("OpenRouter request: %s", requestJson)

        val httpResponse: HttpResponse = client.post("https://openrouter.ai/api/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://howmuch.com.br") // Opcional para OpenRouter
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val responseBody = httpResponse.bodyAsText()
        Timber.tag(TAG).d("OpenRouter response [%s]: %s", httpResponse.status, responseBody)

        return try {
            json.decodeFromString(ChatResponse.serializer(), responseBody)
        } catch (e: Exception) {
            throw IllegalStateException(
                "OpenRouter respondeu ${httpResponse.status} sem 'choices' (model=$model): $responseBody",
                e
            )
        }
    }

    /**
     * Adapta a tool call do formato OpenAI-compatible do OpenRouter para o formato de
     * [BaseAgent.executeAction] (AD-010) e devolve o texto já pronto para a mensagem de role
     * `"tool"`. Não executa nada por si só — quem executa é [BaseAgent.executeAction].
     * `internal` (não `private`) para continuar diretamente testável, como já era antes desta
     * extração (sdd-026, A-03) — ver [OpenRouterAiAgentGatingTest].
     */
    internal suspend fun toToolCallContent(functionCall: FunctionCall, meta: Map<String, Any?>): String {
        val arguments = parseFunctionCallArguments(json, functionCall.arguments)
        return executeAction(functionCall.name, arguments, meta)
    }

    companion object {
        private const val TAG = "OpenRouterAiAgent"
        // Omitting max_tokens lets OpenRouter default to the routed model's own max output
        // (up to 65535 on some), which "auto"/free-tier accounts often can't afford —
        // observed as a real 402 "requires more credits" on this exact request shape. A chat
        // reply plus tool-call round trips never needs anywhere near that; this stays safely
        // under the free-tier credit ceiling seen in production while leaving room for a full
        // multi-paragraph response.
        private const val MAX_RESPONSE_TOKENS = 2_048
    }
}
