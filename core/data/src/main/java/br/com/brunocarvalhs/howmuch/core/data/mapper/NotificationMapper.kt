package br.com.brunocarvalhs.howmuch.core.data.mapper

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
