package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.mapper.toDomain
import br.com.brunocarvalhs.howmuch.core.data.model.NotificationModel
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.service.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.service.make
import br.com.brunocarvalhs.howmuch.core.domain.service.observe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    companion object {
        private const val ENDPOINT = "notifications"
    }
}
