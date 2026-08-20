package br.com.brunocarvalhs.howmuch.core.domain.repository

import kotlinx.coroutines.flow.Flow

data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val timestamp: Long
)

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<Notification>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
}
