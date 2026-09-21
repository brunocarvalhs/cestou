package br.com.brunocarvalhs.howmuch.feature.chat.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ChatMessageModel(
    val id: Long,
    val text: String,
    val sender: String,
    val createdAtEpochMillis: Long
)

// StorageService.save() resolves its serializer from value::class, which for a bare
// List<ChatMessageModel> resolves to ArrayList::class at runtime (not serializable). Wrapping
// the list in a concrete @Serializable class gives it a real, resolvable serializer.
@Serializable
internal data class ChatHistoryModel(
    val messages: List<ChatMessageModel> = emptyList()
)
