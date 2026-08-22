package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class FunctionCall(val name: String, val arguments: String)
