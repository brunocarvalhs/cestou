package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Live count of the signed-in user's unread notifications. It follows the auth state instead of
 * reading the user once, so a ViewModel created before sign-in (or across an account switch) still
 * ends up counting for the right user; 0 while nobody is signed in.
 */
internal class ObserveUnreadNotificationsCountUseCase @Inject constructor(
    private val authService: AuthService,
    private val notificationRepository: NotificationRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Int> = authService.authState
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) flowOf(0) else notificationRepository.observeUnreadCount(userId)
        }
}
