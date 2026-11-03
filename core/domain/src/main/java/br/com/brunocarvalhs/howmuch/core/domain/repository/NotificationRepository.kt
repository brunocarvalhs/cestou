package br.com.brunocarvalhs.howmuch.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * [shoppingId] and [senderId] are null on notifications written before the fields existed, so
 * consumers must treat a missing value as "no list to open" / "unknown sender".
 */
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val timestamp: Long,
    val shoppingId: String? = null,
    val senderId: String? = null
)

/** Where a notification comes from: the list it is about and the member who triggered it. */
data class NotificationOrigin(
    val shoppingId: String,
    val senderId: String
)

/** Result of [NotificationRepository.save]: a second write to an existing id is not an error. */
enum class SaveResult { Saved, AlreadySaved }

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<Notification>>

    /** Emits how many of [userId]'s notifications are unread, reading only the unread documents. */
    fun observeUnreadCount(userId: String): Flow<Int>
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /** Marks every id as read; a failure on one id does not stop the others, and is reported in the result. */
    suspend fun markAllAsRead(notificationIds: List<String>): Result<Unit>

    /**
     * Writes [notification] under its own id. The id is the idempotency key: when that document
     * already exists the result is [SaveResult.AlreadySaved], not a failure.
     */
    suspend fun save(notification: Notification): Result<SaveResult>
    suspend fun notify(
        userId: String,
        title: String,
        message: String,
        type: String,
        origin: NotificationOrigin? = null
    ): Result<Unit>
}
