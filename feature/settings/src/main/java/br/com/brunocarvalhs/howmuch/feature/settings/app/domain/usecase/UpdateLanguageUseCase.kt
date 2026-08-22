package br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(language: String): Result<Unit> = runCatching {
        repository.updateLanguage(language)
    }
}
