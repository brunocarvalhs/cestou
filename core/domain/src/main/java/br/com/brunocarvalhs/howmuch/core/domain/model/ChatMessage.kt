package br.com.brunocarvalhs.howmuch.core.domain.model

import java.time.Instant

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val sender: Sender,
    val createdAt: Instant = Instant.now()
) {
    enum class Sender {
        USER,
        ASSISTANT,
        PARTICIPANT
    }
}
