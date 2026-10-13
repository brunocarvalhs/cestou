package br.com.brunocarvalhs.howmuch.feature.chat.data.mapper

import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.chat.data.model.ChatMessageModel
import java.time.Instant

internal fun ChatMessageModel.toDomain(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    sender = ChatMessage.Sender.valueOf(sender),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
)

internal fun ChatMessage.toModel(): ChatMessageModel = ChatMessageModel(
    id = id,
    text = text,
    sender = sender.name,
    createdAtEpochMillis = createdAt.toEpochMilli()
)
