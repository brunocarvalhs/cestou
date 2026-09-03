package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.extensions.toDomain
import br.com.brunocarvalhs.howmuch.core.data.extensions.toMap
import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        ).map { models -> models?.map { it.toDomain() } ?: emptyList() }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
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

    override suspend fun notify(
        userId: String,
        title: String,
        message: String,
        type: String
    ): Result<Unit> = runCatching {
        val model = NotificationModel(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            message = message,
            type = type,
            isRead = false,
            timestamp = System.currentTimeMillis()
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

    companion object {
        private const val ENDPOINT = "notifications"
    }
}
