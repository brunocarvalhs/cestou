package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import timber.log.Timber
import javax.inject.Inject

private const val SHORT_CODE_MAX_LENGTH = 8

class ShoppingJoinUseCase @Inject constructor(
    private val repository: ShoppingRepository,
    private val authService: AuthService
) {
    suspend operator fun invoke(token: String): Result<Unit> = runCatching {
        Timber.d("Joining shopping with token: $token")
        val userId = authService.getOrCreateUserId().id
        
        val shopping = if (token.length <= SHORT_CODE_MAX_LENGTH) {
            repository.getByShortCode(token.uppercase())
        } else {
            repository.getById(token)
        }

        Timber.d("Shopping found: $shopping")
        requireNotNull(shopping) { "Lista não encontrada para o token informado." }
        
        repository.join(shopping.id, userId)
    }
}
