package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.FunctionCall
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.Message
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model.ToolCall
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionCallPart
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject

/**
 * O histórico da sessão usa o formato do Gemini (partes `functionCall`/`functionResponse`) para
 * os dois providers. Estes helpers convertem as ações de um turno do OpenRouter para esse formato
 * e o histórico de volta para `tool_calls` + mensagens `tool`, sem texto solto no meio da conversa.
 */
internal const val TOOL_ROLE = "tool"

/** O par de entradas de histórico para as [calls] de uma rodada e seus [results], na mesma ordem. */
internal fun toolRoundContents(
    calls: List<Pair<String, Map<String, Any?>>>,
    results: List<String>
): List<Content> = listOf(
    Content(
        role = "model",
        parts = calls.map { (name, args) -> FunctionCallPart(name, args.mapValues { it.value.asHistoryArg() }) }
    ),
    Content(
        role = TOOL_ROLE,
        parts = calls.zip(results).map { (call, result) ->
            FunctionResponsePart(call.first, JSONObject().apply { put("result", result) })
        }
    )
)

private fun Any?.asHistoryArg(): String? = when (this) {
    null -> null
    is JsonPrimitive -> content
    is JsonElement -> toString()
    else -> toString()
}

/**
 * Converte o histórico para mensagens do OpenRouter. Uma rodada de ações só vira `tool_calls` se
 * toda chamada tiver resposta logo em seguida: o provider rejeita `tool_calls` sem a mensagem
 * `tool` correspondente.
 */
internal fun List<Content>.toOpenRouterMessages(): List<Message> {
    val messages = mutableListOf<Message>()
    var index = 0
    var nextId = 0
    while (index < size) {
        val content = this[index]
        val calls = content.parts.filterIsInstance<FunctionCallPart>()
        val responses = this.getOrNull(index + 1)?.parts?.filterIsInstance<FunctionResponsePart>().orEmpty()
        val text = content.parts.filterIsInstance<TextPart>().joinToString(" ") { it.text }
        when {
            calls.isNotEmpty() && calls.size == responses.size -> {
                val ids = calls.map { "call_${nextId++}" }
                messages += Message(
                    role = "assistant",
                    toolCalls = calls.zip(ids).map { (call, id) ->
                        ToolCall(id, "function", FunctionCall(call.name, call.args.toJsonObjectString()))
                    }
                )
                responses.zip(ids).forEach { (response, id) ->
                    messages += Message(
                        role = TOOL_ROLE,
                        toolCallId = id,
                        content = response.response.optString("result")
                    )
                }
                index++
            }
            calls.isNotEmpty() || content.parts.any { it is FunctionResponsePart } -> Unit
            text.isNotEmpty() -> messages += Message(role = openRouterRoleFor(content.role), content = text)
        }
        index++
    }
    return messages
}

private fun Map<String, String?>.toJsonObjectString(): String =
    JsonObject(mapValues { JsonPrimitive(it.value) }).toString()
