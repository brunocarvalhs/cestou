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
    timestamp = timestamp
)

fun NotificationModel.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "title" to title,
    "message" to message,
    "type" to type,
    "isRead" to isRead,
    "timestamp" to timestamp
)
