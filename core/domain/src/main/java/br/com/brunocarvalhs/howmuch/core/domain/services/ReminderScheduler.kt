package br.com.brunocarvalhs.howmuch.core.domain.services

import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings

/**
 * Keeps whatever local reminder mechanism the app uses in sync with the persisted [AppSettings].
 * Exists as a port so `core/data`'s [br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository]
 * implementation can trigger a resync after a settings write without depending on `feature/settings`'s
 * concrete WorkManager-based scheduler (`ShoppingReminderScheduler`), which implements this interface.
 */
interface ReminderScheduler {
    fun sync(settings: AppSettings)
}
