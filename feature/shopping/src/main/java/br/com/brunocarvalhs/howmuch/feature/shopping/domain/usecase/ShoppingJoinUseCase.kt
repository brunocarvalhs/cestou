package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

private const val SHORT_CODE_MAX_LENGTH = 8

class ShoppingJoinUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ShoppingRepository,
    private val authService: AuthService,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(token: String): Result<Unit> = runCatching {
        Timber.d("Joining shopping with token: $token")
        val user = authService.getOrCreateUserId()

        val shopping = if (token.length <= SHORT_CODE_MAX_LENGTH) {
            repository.getByShortCode(token.uppercase())
        } else {
            repository.getById(token)
        }

        Timber.d("Shopping found: $shopping")
        requireNotNull(shopping) { "Lista não encontrada para o token informado." }

        repository.join(shopping.id, user.id)

        val actorName = user.displayName ?: context.getString(CoreR.string.notification_someone)
        val title = context.getString(CoreR.string.notification_list_joined_title)
        val message = context.getString(CoreR.string.notification_list_joined_message, actorName, shopping.title)
        shopping.users.filter { it != user.id }.forEach { memberId ->
            notificationRepository.notify(memberId, title, message, TYPE_LIST_JOINED)
        }
    }

    private companion object {
        const val TYPE_LIST_JOINED = "list_joined"
    }
}
