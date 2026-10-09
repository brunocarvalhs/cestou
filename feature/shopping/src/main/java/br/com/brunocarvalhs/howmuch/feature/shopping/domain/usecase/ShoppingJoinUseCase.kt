package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.ShoppingTexts
import timber.log.Timber
import javax.inject.Inject

private const val SHORT_CODE_MAX_LENGTH = 8

class ShoppingJoinUseCase @Inject constructor(
    private val texts: ShoppingTexts,
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

        val title = texts.listJoinedTitle()
        val message = texts.listJoinedMessage(user.displayName, shopping.title)
        shopping.users.filter { it != user.id }.forEach { memberId ->
            notificationRepository.notify(memberId, title, message, TYPE_LIST_JOINED)
        }
    }

    private companion object {
        const val TYPE_LIST_JOINED = "list_joined"
    }
}
