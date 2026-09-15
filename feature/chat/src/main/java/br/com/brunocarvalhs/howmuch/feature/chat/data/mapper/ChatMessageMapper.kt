package br.com.brunocarvalhs.howmuch.feature.chat.data.mapper

import br.com.brunocarvalhs.howmuch.feature.chat.data.model.ChatMessageModel
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage
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
