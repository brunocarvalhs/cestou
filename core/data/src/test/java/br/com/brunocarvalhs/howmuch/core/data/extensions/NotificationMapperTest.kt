package br.com.brunocarvalhs.howmuch.core.data.extensions

import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationMapperTest {

    @Test
    fun `toDomain maps every field from NotificationModel to Notification`() {
        val model = NotificationModel(
            id = "n1",
            userId = "u1",
            title = "Lista compartilhada",
            message = "Fulano compartilhou uma lista",
            type = "share",
            isRead = true,
            timestamp = 1000L
        )

        val notification = model.toDomain()

        assertEquals(model.id, notification.id)
        assertEquals(model.userId, notification.userId)
        assertEquals(model.title, notification.title)
        assertEquals(model.message, notification.message)
        assertEquals(model.type, notification.type)
        assertEquals(model.isRead, notification.isRead)
        assertEquals(model.timestamp, notification.timestamp)
    }
}
