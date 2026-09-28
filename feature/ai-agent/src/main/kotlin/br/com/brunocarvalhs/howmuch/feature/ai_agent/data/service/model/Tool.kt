package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

// No default on `type`: kotlinx.serialization omits a property that equals its default value
// unless the Json instance sets encodeDefaults = true (it doesn't, here). Some OpenRouter
// backends (observed: Azure) require "type" to be present on every tool and reject the request
// otherwise ("not a valid chat-completions function tool ... expected \"function\"") — making it
// required forces it to always serialize, regardless of the value.
@OptIn(InternalSerializationApi::class)
@Serializable
data class Tool(
    val type: String,
    val function: FunctionDeclaration,
)
