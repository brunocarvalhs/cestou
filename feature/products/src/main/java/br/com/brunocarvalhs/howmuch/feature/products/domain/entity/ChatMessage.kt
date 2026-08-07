package br.com.brunocarvalhs.howmuch.feature.products.domain.model

import androidx.compose.runtime.Stable
import java.time.Instant

@Stable
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
