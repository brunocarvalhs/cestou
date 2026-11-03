package br.com.brunocarvalhs.howmuch.core.data.extensions

import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `toDomain maps shoppingId and senderId`() {
        val notification = NotificationModel(
            "n1", "u1", "t", "m", "list_joined", false, 1L, shoppingId = "s1", senderId = "u2"
        ).toDomain()

        assertEquals("s1", notification.shoppingId)
        assertEquals("u2", notification.senderId)
    }

    @Test
    fun `toDomain keeps optional fields null for legacy models`() {
        val notification = NotificationModel("n1", "u1", "t", "m", "list_joined", false, 1L).toDomain()

        assertNull(notification.shoppingId)
        assertNull(notification.senderId)
    }

    @Test
    fun `toMap includes optional fields only when set`() {
        val full = NotificationModel("n1", "u1", "t", "m", "list_joined", false, 1L, "s1", "u2").toMap()
        val legacy = NotificationModel("n1", "u1", "t", "m", "list_joined", false, 1L).toMap()

        assertEquals("s1", full["shoppingId"])
        assertEquals("u2", full["senderId"])
        assertFalse(legacy.containsKey("shoppingId"))
        assertFalse(legacy.containsKey("senderId"))
        assertEquals(7, legacy.size)
    }
}
