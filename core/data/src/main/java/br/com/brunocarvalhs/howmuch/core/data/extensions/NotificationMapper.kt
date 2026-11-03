package br.com.brunocarvalhs.howmuch.core.data.extensions

import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification

fun NotificationModel.toDomain() = Notification(
    id = id,
    userId = userId,
    title = title,
    message = message,
    type = type,
    isRead = isRead,
    timestamp = timestamp,
    shoppingId = shoppingId,
    senderId = senderId
)

/**
 * The optional fields are left out when null instead of written as null: the Firestore rules pin
 * the key set and a null `shoppingId` would fail their `is string` check.
 */
fun NotificationModel.toMap(): Map<String, Any?> = buildMap {
    put("id", id)
    put("userId", userId)
    put("title", title)
    put("message", message)
    put("type", type)
    put("isRead", isRead)
    put("timestamp", timestamp)
    shoppingId?.let { put("shoppingId", it) }
    senderId?.let { put("senderId", it) }
}
