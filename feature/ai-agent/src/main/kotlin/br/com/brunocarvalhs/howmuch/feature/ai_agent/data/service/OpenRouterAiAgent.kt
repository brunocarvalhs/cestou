package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.ChatRequest
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.ChatResponse
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.FunctionCall
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.FunctionDeclaration
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.Message
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.Tool
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber

/**
 * Implementação do Agente utilizando a API do OpenRouter.
 * Suporta múltiplos modelos (como GPT-4, Claude, etc) e Function Calling.
 */
internal class OpenRouterAiAgent(
    private val session: AiSession,
    private val registry: AgentRegistry = AgentRegistry,
    private val model: String = BuildConfig.OPEN_ROUTER_MODEL,
    private val apiKey: String = BuildConfig.OPEN_ROUTER_API_KEY
) : AiAgent {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
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
            messages.add(Message(role = "system", content = """
                Você é o Cestou Assistant, um assistente especializado em ajudar o usuário com listas de compras e produtos.
                ... (Diretrizes iguais ao Gemini) ...
            """.trimIndent()))

            // Converte histórico do session (Gemini Content format) para Message format
            session.history.forEach { content ->
                val text = content.parts.filterIsInstance<TextPart>().joinToString(" ") { it.text }
                if (text.isNotEmpty()) {
                    messages.add(Message(role = content.role ?: "user", content = text))
                }
            }

            // Adiciona mensagem atual
            messages.add(Message(role = "user", content = prompt))

            val tools = registry.getAll().map { action ->
                Tool(
                    function = FunctionDeclaration(
                        name = action.id,
                        description = action.description,
                        parameters = JsonObject(action.parameters.associate { param ->
                            param.name to JsonObject(mapOf(
                                "type" to JsonPrimitive(param.type),
                                "description" to JsonPrimitive(param.description)
                            ))
                        })
                    )
                )
            }

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
                            // Converte os argumentos de String (JSON) para Map
                            val args = Json.decodeFromString<Map<String, Any?>>(functionCall.arguments)
                            action.execute(args, session, meta).getOrNull()?.toString() ?: "Sucesso"
                        } catch (e: Exception) {
                            "Erro na execução da ação: ${e.message}"
                        }
                    } else {
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

            response.choices.firstOrNull()?.message?.content?.let { text ->
                emit(text)
                // Atualiza o histórico (simplificado para demonstração)
                session.history.add(Content(role = "user", parts = listOf(TextPart(prompt))))
                session.history.add(Content(role = "model", parts = listOf(TextPart(text))))
            }

        } catch (e: Exception) {
            Timber.e(e, "Erro na comunicação com OpenRouter")
            emit("Desculpe, tive um problema ao processar sua solicitação no OpenRouter.")
        }
    }

    private suspend fun executeRequest(request: ChatRequest): ChatResponse {
        return client.post("https://openrouter.ai/api/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://howmuch.com.br") // Opcional para OpenRouter
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
