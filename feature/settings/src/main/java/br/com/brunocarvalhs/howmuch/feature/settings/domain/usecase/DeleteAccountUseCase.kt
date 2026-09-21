package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

// Firestore cleanup must happen before deleteAccount(): security rules require an authenticated request.
internal class DeleteAccountUseCase @Inject constructor(
    private val authService: AuthService,
    private val shoppingRepository: ShoppingRepository,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val userId = authService.currentUser?.id
            ?: return Result.failure(IllegalStateException("No authenticated user"))

        return runCatching {
            shoppingRepository.getAll().forEach { shopping ->
                if (shopping.users.size <= 1) {
                    shoppingRepository.delete(shopping)
                } else {
                    shoppingRepository.update(shopping.copy(users = shopping.users - userId))
                }
            }
            userRepository.deleteProfile(userId).getOrThrow()
            settingsRepository.deleteAllData()
            authService.deleteAccount().getOrThrow()
        }
    }
}
