package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import javax.inject.Inject

/** Marks as read only the notifications that are still unread; returns how many were sent. */
internal class MarkAllNotificationsReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(notifications: List<Notification>): Result<Int> {
        val unreadIds = notifications.filterNot { it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return Result.success(0)
        return notificationRepository.markAllAsRead(unreadIds).map { unreadIds.size }
    }
}
