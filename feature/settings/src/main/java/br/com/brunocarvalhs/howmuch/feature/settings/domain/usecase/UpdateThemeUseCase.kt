package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.entity.ThemeMode
import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateThemeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(themeMode: ThemeMode): Result<Unit> = runCatching {
        repository.updateTheme(themeMode)
    }
}
