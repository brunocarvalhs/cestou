package br.com.brunocarvalhs.howmuch.core.ai.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.ai.service.model.ChatRequest
import br.com.brunocarvalhs.howmuch.core.ai.service.model.ChatResponse
import br.com.brunocarvalhs.howmuch.core.ai.service.model.FunctionDeclaration
import br.com.brunocarvalhs.howmuch.core.ai.service.model.Message
import br.com.brunocarvalhs.howmuch.core.ai.service.model.Tool
import br.com.brunocarvalhs.howmuch.core.ai.service.model.ToolCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * Agente de IA que utiliza a API do OpenRouter com suporte a Tool Calling.
 */
class OpenRouterAiAgent(
    private val session: AiAgentSession,
    private val registry: AgentRegistry = AgentRegistry,
    private val modelName: String = BuildConfig.OPEN_ROUTER_MODEL,
    private val customPrompt: String? = null,
    private val temperature: Float = 0.7f,
) : AiAgent {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }
    }

    override suspend fun sendMessage(
        prompt: String,
        context: AiAgentContext,
    ): Flow<String> = flow {
        val meta = context.toMetadata()
        try {
            val messages = mutableListOf(
                Message(role = "system", content = createSystemPrompt(meta)),
                Message(role = "user", content = prompt)
            )

            var iterations = 0
            while (iterations++ < MAX_ITERATIONS) {
                val assistantMessage = executeChatRequest(messages) ?: break
                messages.add(assistantMessage)

                val toolCalls = assistantMessage.toolCalls
                if (toolCalls.isNullOrEmpty()) {
                    assistantMessage.content?.let { emit(it) }
                    break
                }

                executeToolCalls(toolCalls, session, meta, messages)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Falha no Agente")
            throw e
        }
    }

    private fun createSystemPrompt(meta: Map<String, Any?>): String {
        return customPrompt ?: """
            Você é o Cestou Assistant, um assistente inteligente para gestão de compras.
            Idioma: ${session.locale.displayLanguage}
            Contexto atual: $meta
            
            Diretrizes:
            1. Priorize o 'shoppingId' presente no contexto para ações em listas.
            2. Se precisar de uma informação que não está no contexto, chame a ferramenta de busca adequada.
            3. Responda de forma concisa e amigável.
        """.trimIndent()
    }

    private suspend fun executeChatRequest(messages: List<Message>): Message? {
        val body = ChatRequest(
            model = modelName,
            messages = messages,
            temperature = temperature,
            tools = registry.getTools()
        )

        val httpResponse = client.post(BuildConfig.OPEN_ROUTER_BASE_URL) {
            header("Authorization", "Bearer ${BuildConfig.OPEN_ROUTER_API_KEY}")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (!httpResponse.status.isSuccess()) throw Exception("Erro API: ${httpResponse.status}")

        val response: ChatResponse = httpResponse.body()
        return response.choices.firstOrNull()?.message
    }

    private suspend fun executeToolCalls(
        toolCalls: List<ToolCall>,
        session: AiAgentSession,
        meta: Map<String, Any?>,
        messages: MutableList<Message>
    ) {
        toolCalls.forEach { toolCall ->
            val action = registry.find(toolCall.function.name)
            val result = if (action != null) {
                try {
                    val arguments = Json.parseToJsonElement(toolCall.function.arguments).jsonObject
                    action.execute(arguments, session, meta).getOrNull()?.toString() ?: "Sucesso"
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    "Erro: ${e.message}"
                }
            } else {
                "Ação não encontrada"
            }
            messages.add(Message(role = "tool", toolCallId = toolCall.id, content = result))
        }
    }

    private fun AgentRegistry.getTools(): List<Tool> = getAll().map { action ->
        Tool(
            function = FunctionDeclaration(
                name = action.id,
                description = action.description,
                parameters = action.parameters.toJsonSchema()
            )
        )
    }

    private fun List<AiAgentParameter>.toJsonSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        putJsonObject("properties") {
            forEach { param ->
                putJsonObject(param.name) {
                    put("type", JsonPrimitive(param.type))
                    put("description", JsonPrimitive(param.description))
                }
            }
        }
        putJsonArray("required") {
            forEach { if (it.isRequired) add(JsonPrimitive(it.name)) }
        }
    }

    companion object {
        private const val REQUEST_TIMEOUT_MILLIS = 120_000L
        private const val CONNECT_TIMEOUT_MILLIS = 60_000L
        private const val MAX_ITERATIONS = 10
    }
}
