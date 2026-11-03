package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.extensions.toDomain
import br.com.brunocarvalhs.howmuch.core.data.extensions.toMap
import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationOrigin
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val networkService: NetworkService
) : NotificationRepository {

    override fun observeNotifications(userId: String): Flow<List<Notification>> {
        return networkService.observe<List<NotificationModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.GET,
                query = mapOf("userId" to userId)
            )
        ).retryWithBackoff().catch { e ->
            Timber.e(e, "Error observing notifications for userId: %s", userId)
            emit(emptyList())
        }.map { models ->
            models?.map { it.toDomain() } ?: emptyList()
        }
    }

    override fun observeUnreadCount(userId: String): Flow<Int> {
        return networkService.observe<List<NotificationModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.GET,
                query = mapOf("userId" to userId, "isRead" to false)
            )
        ).retryWithBackoff().catch { e ->
            Timber.e(e, "Error observing unread notifications for userId: %s", userId)
            emit(emptyList())
        }.map { models -> models?.size ?: 0 }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatchingCancellable {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/$notificationId",
                method = NetworkService.Method.PUT,
                payload = mapOf("isRead" to true)
            ),
            response = Boolean::class
        )
        Unit
    }

    override suspend fun markAllAsRead(notificationIds: List<String>): Result<Unit> {
        if (notificationIds.isEmpty()) return Result.success(Unit)
        val results = coroutineScope {
            notificationIds.chunked(MARK_ALL_BATCH_SIZE).flatMap { batch ->
                batch.map { id -> async { markAsRead(id) } }.awaitAll()
            }
        }
        return results.firstOrNull { it.isFailure } ?: Result.success(Unit)
    }

    override suspend fun notify(
        userId: String,
        title: String,
        message: String,
        type: String,
        origin: NotificationOrigin?
    ): Result<Unit> = runCatchingCancellable {
        val model = NotificationModel(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            message = message,
            type = type,
            isRead = false,
            timestamp = System.currentTimeMillis(),
            shoppingId = origin?.shoppingId,
            senderId = origin?.senderId
        )
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.POST,
                payload = model.toMap()
            ),
            response = String::class
        )
        Unit
    }

    /**
     * A Firestore snapshot listener closes for good on its first error (offline start, a token
     * refresh racing the first read), which would freeze the badge and the feed. Re-subscribe a few
     * times with a growing delay before giving up to the caller's empty fallback.
     */
    private fun <T> Flow<T>.retryWithBackoff(): Flow<T> = retryWhen { cause, attempt ->
        if (cause is CancellationException || attempt >= MAX_OBSERVE_RETRIES) {
            false
        } else {
            delay(RETRY_BASE_DELAY_MS shl attempt.toInt())
            true
        }
    }

    /** Like [runCatching], but a cancelled coroutine stays cancelled instead of becoming a failed [Result]. */
    private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
        runCatching(block).onFailure { if (it is CancellationException) throw it }

    companion object {
        private const val ENDPOINT = "notifications"
        private const val MARK_ALL_BATCH_SIZE = 10
        private const val MAX_OBSERVE_RETRIES = 3L
        private const val RETRY_BASE_DELAY_MS = 2_000L
    }
}
