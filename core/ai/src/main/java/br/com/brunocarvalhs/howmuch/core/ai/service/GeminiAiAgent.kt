package br.com.brunocarvalhs.howmuch.core.ai.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.Tool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import timber.log.Timber

/**
 * Implementação do Agente utilizando Google Gemini AI SDK.
 */
class GeminiAiAgent(
    private val session: AiAgentSession,
    private val registry: AgentRegistry = AgentRegistry,
    private val modelName: String = BuildConfig.GEMINI_AGENT,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) : AiAgent {

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            systemInstruction = Content(
                role = "system",
                parts = listOf(
                    TextPart(
                        """
                        Você é o Cestou Assistant, um assistente especializado em ajudar o usuário com listas de compras e produtos.
                        Diretrizes de comportamento:
                        1. Priorize a lista de compras atual fornecida no contexto (através do metadata) ao adicionar produtos, a menos que o usuário peça explicitamente para criar uma nova lista.
                        2. Se um ID de lista de compras ('shopping') estiver presente no contexto, use-o como padrão para ações que exigem um 'shoppingId'.
                        3. Ao adicionar produtos, use as seguintes categorias padrão: Hortifruti, Carnes, Laticínios, Bebidas, Limpeza, Higiene, Mercearia, Legumes, Perecíveis, Congelados, Padaria ou Outros.
                        4. Suporte quantidades decimais (ex: 0.5 para meio quilo) e use unidades de medida como 'kg', 'g', 'L', 'ml', 'un', 'pct', 'cx'.
                        5. Se a lista tiver um orçamento ('budget'), avise o usuário se os itens adicionados ultrapassarem esse valor.
                        6. Seja conciso e útil.
                        """.trimIndent()
                    )
                )
            ),
            tools = listOf(
                Tool(
                    functionDeclarations = registry.getAll().map { action ->
                        FunctionDeclaration(
                            name = action.id,
                            description = action.description,
                            parameters = action.parameters.map { param ->
                                when (param.type) {
                                    "integer" -> Schema.int(param.name, param.description)
                                    "number" -> Schema.double(param.name, param.description)
                                    "boolean" -> Schema.bool(param.name, param.description)
                                    else -> Schema.str(param.name, param.description)
                                }
                            },
                            requiredParameters = action.parameters.filter { it.isRequired }
                                .map { it.name }
                        )
                    }
                )
            ),
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
            )
        )
    }

    override suspend fun sendMessage(
        prompt: String,
        context: AiAgentContext
    ): Flow<String> = flow {
        val chat = generativeModel.startChat(history = session.history)
        val meta = context.toMetadata()

        try {
            var response = chat.sendMessage(prompt)

            // Loop para processar Function Calling
            while (response.functionCalls.isNotEmpty()) {
                val toolResponses = response.functionCalls.map { functionCall ->
                    val action = registry.find(functionCall.name)
                    val result = if (action != null) {
                        try {
                            action.execute(
                                arguments = functionCall.args,
                                session = session,
                                metadata = meta
                            ).getOrNull()?.toString() ?: "Sucesso"
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            "Erro na execução da ação: ${e.message}"
                        }
                    } else {
                        "Erro: Ação '${functionCall.name}' não encontrada"
                    }

                    // Cria a parte de resposta para o Gemini
                    FunctionResponsePart(
                        name = functionCall.name,
                        response = JSONObject().apply { put("result", result) }
                    )
                }

                // Cria o conteúdo da resposta das ferramentas e envia de volta
                val toolContent = Content(role = "tool", parts = toolResponses)
                response = chat.sendMessage(toolContent)
            }

            response.text?.let {
                emit(it)
                // Atualiza o histórico da sessão com o estado atual do chat
                session.history.clear()
                session.history.addAll(chat.history)
            }

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.e(e, "Erro na comunicação com Gemini")
            emit("Desculpe, tive um problema ao processar sua solicitação no Gemini.")
        }
    }
}
