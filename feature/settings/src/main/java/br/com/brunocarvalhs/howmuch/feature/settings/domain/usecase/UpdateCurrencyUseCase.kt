package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateCurrencyUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(currency: String): Result<Unit> = runCatching {
        repository.updateCurrency(currency)
    }
}
