package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class SetLimitCardUseCase @Inject constructor(
    private val cartLocalStorage: ICartLocalStorage,
) {
    suspend operator fun invoke(limit: Long): Result<Unit> = runCatching {
        cartLocalStorage.saveCartLimit(limit)
    }
}