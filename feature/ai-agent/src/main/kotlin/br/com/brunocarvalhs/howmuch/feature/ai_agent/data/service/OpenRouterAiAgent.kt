package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.ChatRequest
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.ChatResponse
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.Message
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Implementação do Agente utilizando a API do OpenRouter.
 * Suporta múltiplos modelos (como GPT-4, Claude, etc) e Function Calling.
 */
internal class OpenRouterAiAgent(
    private val session: AiSession,
    private val registry: AgentRegistry = AgentRegistry,
    private val model: String = BuildConfig.OPEN_ROUTER_MODEL,
    private val apiKey: String = BuildConfig.OPEN_ROUTER_API_KEY,
    private val crashReporter: CrashReporter,
    private val systemPrompt: String = SystemPrompts.CESTOU_ASSISTANT
) : AiAgent {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        // Free-tier OpenRouter models can take a while to respond (routing through "auto",
        // cold-starting a provider, etc.) — the engine's short default timeout was causing
        // real requests to be cut off as SocketTimeoutException before they finished.
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }

    override suspend fun sendMessage(
        prompt: String,
        context: AiAgentContext
    ): Flow<String> = flow {
        val meta = context.toMetadata()

        try {
            // Converte o histórico e o prompt atual para o formato de mensagens do OpenRouter
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

            val tools = buildOpenRouterTools(registry.getAll())

            var currentRequest = ChatRequest(model = model, messages = messages, tools = tools)
            var response: ChatResponse = executeRequest(currentRequest)

            // Loop para processar Function Calling
            while (response.choices.firstOrNull()?.message?.toolCalls?.isNotEmpty() == true) {
                val assistantMessage = response.choices.first().message
                messages.add(assistantMessage)

                assistantMessage.toolCalls?.forEach { toolCall ->
                    val functionCall = toolCall.function
                    val action = registry.find(functionCall.name)
                    
                    val result = if (action != null) {
                        try {
                            val args = parseFunctionCallArguments(json, functionCall.arguments)
                            action.execute(args, session, meta).getOrNull()?.toString() ?: "Sucesso"
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(
                                e,
                                "Erro executando a função '%s' com args=%s",
                                functionCall.name,
                                functionCall.arguments
                            )
                            crashReporter.recordException(
                                e,
                                extras = mapOf(
                                    "provider" to "openrouter",
                                    "model" to model,
                                    "function" to functionCall.name
                                )
                            )
                            "Erro na execução da ação: ${e.message}"
                        }
                    } else {
                        Timber.tag(TAG).w("Ação '%s' não encontrada no registry", functionCall.name)
                        "Erro: Ação '${functionCall.name}' não encontrada"
                    }

                    messages.add(Message(
                        role = "tool",
                        toolCallId = toolCall.id,
                        content = result
                    ))
                }

                currentRequest = ChatRequest(model = model, messages = messages, tools = tools)
                response = executeRequest(currentRequest)
            }

            response.choices.firstOrNull()?.message?.content?.trim()?.let { text ->
                emit(text)
                // Atualiza o histórico (simplificado para demonstração)
                session.history.add(Content(role = "user", parts = listOf(TextPart(prompt))))
                session.history.add(Content(role = "model", parts = listOf(TextPart(text))))
                session.history.trimToRecentHistory()
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro na comunicação com OpenRouter (model=%s)", model)
            crashReporter.recordException(
                e,
                extras = mapOf(
                    "provider" to "openrouter",
                    "model" to model
                )
            )
            emit(aiErrorMessageFor(e))
        }
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

    companion object {
        private const val TAG = "OpenRouterAiAgent"
        private const val REQUEST_TIMEOUT_MS = 45_000L
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }
}
