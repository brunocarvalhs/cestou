package br.com.brunocarvalhs.howmuch.feature.chat.data.mapper

import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.chat.data.model.ChatMessageModel
import timber.log.Timber
import java.time.Instant

/**
 * Returns null instead of throwing when [sender] is an unknown value (legacy or corrupted
 * persisted history, crash-hardening sdd-005 C-04) so the caller can drop that one message.
 */
internal fun ChatMessageModel.toDomain(): ChatMessage? {
    val parsedSender = runCatching { ChatMessage.Sender.valueOf(sender) }.getOrNull()
    if (parsedSender == null) {
        Timber.w("Discarding chat message %s with unknown sender '%s'", id, sender)
        return null
    }
    return ChatMessage(
        id = id,
        text = text,
        sender = parsedSender,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
    )
}

internal fun ChatMessage.toModel(): ChatMessageModel = ChatMessageModel(
    id = id,
    text = text,
    sender = sender.name,
    createdAtEpochMillis = createdAt.toEpochMilli()
)
