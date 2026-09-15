package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.FunctionDeclaration
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.Tool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pulled out of [OpenRouterAiAgent] so the mapping between our internal models and OpenRouter's
 * OpenAI-compatible request/response shape can be unit tested without spinning up an HTTP client.
 * Every function here corresponds to a bug that made every OpenRouter conversation fail in
 * practice — regression tests for these should stay in lockstep with any future change.
 */

/**
 * [AiSession.history] is shared with [GeminiAiAgent] and stores roles using the Gemini SDK's
 * convention ("user"/"model"). OpenRouter/OpenAI-compatible providers reject "model" outright
 * (they expect "assistant"), which used to fail every multi-turn OpenRouter request outright.
 */
internal fun openRouterRoleFor(historyRole: String?): String =
    if (historyRole == "model") "assistant" else historyRole ?: "user"

/**
 * Builds the OpenAI-compatible `tools` array from the app's [AgentAction] registry.
 *
 * `parameters` must be a full JSON Schema object (`{"type":"object","properties":{...}}`), not a
 * bare map of `paramName -> schema`: providers validate this strictly, and a param literally
 * named "title" (create_shopping_list) used to collide with the JSON Schema "title" keyword at
 * the top level and get rejected outright ("... is not of type 'string'").
 */
internal fun buildOpenRouterTools(actions: List<AgentAction<*>>): List<Tool> = actions.map { action ->
    Tool(
        function = FunctionDeclaration(
            name = action.id,
            description = action.description,
            parameters = JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(action.parameters.associate { param ->
                        param.name to JsonObject(mapOf(
                            "type" to JsonPrimitive(param.type),
                            "description" to JsonPrimitive(param.description)
                        ))
                    }),
                    "required" to JsonArray(
                        action.parameters.filter { it.isRequired }.map { JsonPrimitive(it.name) }
                    )
                )
            )
        )
    )
}

/**
 * Decodes a tool call's raw JSON arguments into the `Map<String, Any?>` [AgentAction.execute]
 * expects. `Map<String, Any?>` has no kotlinx.serialization serializer of its own (`Any` isn't
 * serializable) — decoding straight into it blew up with "Serializer for class 'Any' is not
 * found" on every single call that had arguments, silently failing every AI-initiated action.
 * `Map<String, JsonElement>` is natively supported, and the `AgentAction.getString/getInt/...`
 * helpers already unwrap a `JsonElement.toString()` representation.
 */
internal fun parseFunctionCallArguments(json: Json, argumentsJson: String): Map<String, Any?> =
    json.decodeFromString<Map<String, JsonElement>>(argumentsJson)
