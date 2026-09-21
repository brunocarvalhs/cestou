package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.BuildConfig
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionCallPart
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
internal class GeminiAiAgent(
    dependencies: AiAgentDependencies,
    private val modelName: String = BuildConfig.GEMINI_AGENT,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) : AiAgent {

    private val session: AiSession = dependencies.session
    private val registry: AgentRegistry = dependencies.registry
    private val crashReporter: CrashReporter = dependencies.crashReporter
    private val systemPrompt: String = dependencies.systemPrompt

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            systemInstruction = Content(
                role = "system",
                parts = listOf(TextPart(systemPrompt))
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
            // systemInstruction is bound once when generativeModel is built (see below), so a
            // per-request value like the current shoppingId can't go there — it rides along on
            // the turn's own prompt instead, which the model reads exactly the same way.
            var response = chat.sendMessage(prompt + SystemPrompts.contextSuffix(meta))

            // Loop para processar Function Calling
            while (response.functionCalls.isNotEmpty()) {
                val toolResponses = response.functionCalls.map { functionCall ->
                    executeFunctionCall(functionCall, meta)
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
                    "provider" to "gemini",
                    "model" to modelName
                )
            )
            emit(aiErrorMessageFor(e))
        }
    }

    private suspend fun executeFunctionCall(
        functionCall: FunctionCallPart,
        meta: Map<String, Any?>
    ): FunctionResponsePart {
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
                Timber.tag(TAG).e(
                    e,
                    "Erro executando a função '%s' com args=%s",
                    functionCall.name,
                    functionCall.args
                )
                crashReporter.recordException(
                    e,
                    extras = mapOf(
                        "provider" to "gemini",
                        "model" to modelName,
                        "function" to functionCall.name
                    )
                )
                "Erro na execução da ação: ${e.message}"
            }
        } else {
            Timber.tag(TAG).w("Ação '%s' não encontrada no registry", functionCall.name)
            "Erro: Ação '${functionCall.name}' não encontrada"
        }

        // Cria a parte de resposta para o Gemini
        return FunctionResponsePart(
            name = functionCall.name,
            response = JSONObject().apply { put("result", result) }
        )
    }

    companion object {
        private const val TAG = "GeminiAiAgent"
    }
}
