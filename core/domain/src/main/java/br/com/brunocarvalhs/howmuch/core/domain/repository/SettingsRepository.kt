package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateTheme(themeMode: ThemeMode)
    suspend fun updateNotificationSettings(enabled: Boolean, reminderTime: String)
    suspend fun updateAiSettings(
        tier: AiConfigTier,
        model: String,
        creativity: Float,
        customApiKey: String?
    )
    suspend fun updateShoppingPreferences(defaultListId: String?, sortingMode: String, remindersEnabled: Boolean)
    suspend fun updateLanguage(language: String)
    suspend fun updateCurrency(currency: String)
    suspend fun updateAnalyticsEnabled(enabled: Boolean)
    suspend fun clearCache()
    suspend fun deleteAllData()
}
