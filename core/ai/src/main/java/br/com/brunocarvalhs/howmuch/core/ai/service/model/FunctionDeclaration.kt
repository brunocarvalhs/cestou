package br.com.brunocarvalhs.howmuch.core.ai.service.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@OptIn(InternalSerializationApi::class)
@Serializable
data class FunctionDeclaration(val name: String, val description: String, val parameters: JsonObject)
