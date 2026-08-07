package br.com.brunocarvalhs.howmuch.feature.settings.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.*
import br.com.brunocarvalhs.howmuch.core.domain.entity.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.entity.ThemeMode
import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

internal class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
        val CREATIVITY_LEVEL = floatPreferencesKey("creativity_level")
        val DEFAULT_LIST_ID = stringPreferencesKey("default_list_id")
        val SORTING_MODE = stringPreferencesKey("sorting_mode")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY = stringPreferencesKey("currency")
    }

    override fun getSettings(): Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeMode = try {
                ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }

            AppSettings(
                themeMode = themeMode,
                aiModel = preferences[PreferencesKeys.AI_MODEL] ?: "google/gemini-2.0-flash-001",
                customPrompt = preferences[PreferencesKeys.CUSTOM_PROMPT],
                creativityLevel = preferences[PreferencesKeys.CREATIVITY_LEVEL] ?: 0.7f,
                defaultListId = preferences[PreferencesKeys.DEFAULT_LIST_ID],
                sortingMode = preferences[PreferencesKeys.SORTING_MODE] ?: "CATEGORY",
                remindersEnabled = preferences[PreferencesKeys.REMINDERS_ENABLED] ?: false,
                language = preferences[PreferencesKeys.LANGUAGE] ?: "pt",
                currency = preferences[PreferencesKeys.CURRENCY] ?: "BRL"
            )
        }

    override suspend fun updateTheme(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun updateNotificationSettings(enabled: Boolean, reminderTime: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
            preferences[PreferencesKeys.REMINDER_TIME] = reminderTime
        }
    }

    override suspend fun updateAiSettings(model: String, prompt: String?, creativity: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_MODEL] = model
            if (prompt != null) {
                preferences[PreferencesKeys.CUSTOM_PROMPT] = prompt
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_PROMPT)
            }
            preferences[PreferencesKeys.CREATIVITY_LEVEL] = creativity
        }
    }

    override suspend fun updateShoppingPreferences(
        defaultListId: String?,
        sortingMode: String,
        remindersEnabled: Boolean
    ) {
        dataStore.edit { preferences ->
            if (defaultListId != null) {
                preferences[PreferencesKeys.DEFAULT_LIST_ID] = defaultListId
            } else {
                preferences.remove(PreferencesKeys.DEFAULT_LIST_ID)
            }
            preferences[PreferencesKeys.SORTING_MODE] = sortingMode
            preferences[PreferencesKeys.REMINDERS_ENABLED] = remindersEnabled
        }
    }

    override suspend fun updateLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    override suspend fun updateCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    override suspend fun clearCache() {
        context.cacheDir.deleteRecursively()
    }

    override suspend fun deleteAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        clearCache()
    }
}
