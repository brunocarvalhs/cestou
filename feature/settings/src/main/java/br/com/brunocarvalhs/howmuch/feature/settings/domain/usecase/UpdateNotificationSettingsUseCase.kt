package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

internal class UpdateNotificationSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean, reminderTime: String) {
        repository.updateNotificationSettings(enabled, reminderTime)
    }
}
