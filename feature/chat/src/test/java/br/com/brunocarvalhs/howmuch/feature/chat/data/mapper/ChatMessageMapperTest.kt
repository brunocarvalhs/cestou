package br.com.brunocarvalhs.howmuch.feature.chat.data.mapper

import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.chat.data.model.ChatMessageModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ChatMessageMapperTest {

    @Test
    fun `toDomain maps a message with a known sender`() {
        val model = ChatMessageModel(id = 1, text = "oi", sender = "USER", createdAtEpochMillis = 1_000L)

        val result = model.toDomain()

        assertEquals(ChatMessage.Sender.USER, result?.sender)
        assertEquals("oi", result?.text)
    }

    @Test
    fun `toDomain returns null instead of throwing for an unknown sender`() {
        val model = ChatMessageModel(id = 1, text = "corrupted", sender = "INVALID", createdAtEpochMillis = 1_000L)

        assertNull(model.toDomain())
    }

    @Test
    fun `toModel round-trips back to the same sender name`() {
        val message = ChatMessage(
            id = 1,
            text = "oi",
            sender = ChatMessage.Sender.ASSISTANT,
            createdAt = Instant.ofEpochMilli(CREATED_AT_EPOCH_MILLIS)
        )

        val model = message.toModel()

        assertEquals("ASSISTANT", model.sender)
        assertEquals(message, model.toDomain())
    }
}

private const val CREATED_AT_EPOCH_MILLIS = 1_000L
