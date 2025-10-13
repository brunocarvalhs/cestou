package br.com.brunocarvalhs.domain.usecases.cart

import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import javax.inject.Inject

class GetLimitCardUseCase @Inject constructor(
    private val cartLocalStorage: ICartLocalStorage,
) {
    suspend operator fun invoke(): Result<Long> = runCatching {
        cartLocalStorage.getCartLimit()
    }
}