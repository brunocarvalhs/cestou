package br.com.brunocarvalhs.howmuch.core.ai.service.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class Choice(
    val message: Message,
)
