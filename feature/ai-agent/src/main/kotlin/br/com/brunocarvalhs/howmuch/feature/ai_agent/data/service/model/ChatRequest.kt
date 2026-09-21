package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val tools: List<Tool>? = null,
    val temperature: Float? = null,
)
