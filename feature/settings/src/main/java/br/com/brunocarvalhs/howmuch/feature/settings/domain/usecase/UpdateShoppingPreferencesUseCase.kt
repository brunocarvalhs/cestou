package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateShoppingPreferencesUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(defaultListId: String?, sortingMode: String, remindersEnabled: Boolean) {
        repository.updateShoppingPreferences(defaultListId, sortingMode, remindersEnabled)
    }
}
