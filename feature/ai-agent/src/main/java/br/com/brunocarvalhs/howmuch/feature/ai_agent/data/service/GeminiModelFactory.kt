package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.Tool
import javax.inject.Inject

/**
 * Isola a construção do [GenerativeModel] do SDK do Gemini atrás de uma interface injetável, para
 * que [GeminiAiAgent] seja testável sem precisar de uma instância real do SDK (sdd-026, A-03) —
 * antes, o client era montado inline dentro do próprio agente, o que o excluía do gate de
 * cobertura do Kover (`howmuch.kover.gradle.kts`).
 */
internal interface GeminiModelFactory {

    /**
     * Monta um [GenerativeModel] pronto para uso: [systemPrompt] vira a `systemInstruction` e
     * [tools] é convertida para o catálogo de `FunctionDeclaration` que o Gemini usa para decidir
     * quando chamar uma tool. [modelName]/[apiKey] vêm de [br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings]
     * (ou do `BuildConfig`, se o usuário não tiver escolhido um modelo específico).
     */
    fun create(
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        tools: List<AgentAction<*>>
    ): GenerativeModel
}

internal class GeminiModelFactoryImpl @Inject constructor() : GeminiModelFactory {

    override fun create(
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        tools: List<AgentAction<*>>
    ): GenerativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        systemInstruction = Content(
            role = "system",
            parts = listOf(TextPart(systemPrompt))
        ),
        tools = listOf(
            Tool(
                functionDeclarations = tools.map { action ->
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
