package br.com.brunocarvalhs.howmuch.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

// Mantido em sincronia manualmente com AppSettings.aiModel (core:domain não pode referenciar
// core:data, e vice-versa só por esta constante não compensaria o acoplamento extra).
private const val DEFAULT_AI_MODEL = "openrouter/free"

private object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val REMINDER_TIME = stringPreferencesKey("reminder_time")
    val AI_CONFIG_TIER = stringPreferencesKey("ai_config_tier")
    val AI_PROVIDER = stringPreferencesKey("ai_provider")
    val AI_MODEL = stringPreferencesKey("ai_model")
    val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
    val CREATIVITY_LEVEL = floatPreferencesKey("creativity_level")
    val DEFAULT_LIST_ID = stringPreferencesKey("default_list_id")
    val SORTING_MODE = stringPreferencesKey("sorting_mode")
    val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val LANGUAGE = stringPreferencesKey("language")
    val CURRENCY = stringPreferencesKey("currency")
}

private fun Preferences.toAppSettings(): AppSettings {
    val themeMode = try {
        ThemeMode.valueOf(this[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
    } catch (e: IllegalArgumentException) {
        ThemeMode.SYSTEM
    }
    val aiConfigTier = try {
        AiConfigTier.valueOf(this[PreferencesKeys.AI_CONFIG_TIER] ?: AiConfigTier.DEFAULT.name)
    } catch (e: IllegalArgumentException) {
        AiConfigTier.DEFAULT
    }
    // Um `ai_model` salvo que não está mais no catálogo atual é de uma versão antiga do app
    // (único jeito de escolher um modelo é o dropdown em AiSettingsScreen, não há texto livre) —
    // tipicamente "openrouter/auto", que nunca foi de graça (é o Auto Router pago do
    // OpenRouter) e por isso sempre batia 402 pra quem nunca trocou o modelo manualmente.
    val storedModel = this[PreferencesKeys.AI_MODEL]
    val aiModel = if (storedModel.isNullOrBlank() || storedModel == "openrouter/auto") {
        DEFAULT_AI_MODEL
    } else {
        storedModel
    }

    return AppSettings(
        themeMode = themeMode,
        notificationsEnabled = this[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
        reminderTime = this[PreferencesKeys.REMINDER_TIME] ?: "18:00",
        aiConfigTier = aiConfigTier,
        aiProvider = resolveStoredAiProvider(this[PreferencesKeys.AI_PROVIDER], aiModel),
        aiModel = aiModel,
        customApiKey = this[PreferencesKeys.CUSTOM_API_KEY],
        creativityLevel = this[PreferencesKeys.CREATIVITY_LEVEL] ?: 0.7f,
        defaultListId = this[PreferencesKeys.DEFAULT_LIST_ID],
        sortingMode = this[PreferencesKeys.SORTING_MODE] ?: "CATEGORY",
        remindersEnabled = this[PreferencesKeys.REMINDERS_ENABLED] ?: false,
        language = this[PreferencesKeys.LANGUAGE] ?: "pt-BR",
        currency = this[PreferencesKeys.CURRENCY] ?: "BRL"
    )
}

/**
 * [storedProvider] pode ser um valor legado (o fabricante, ex. "nvidia") de antes de
 * [AiModel.vendor]/[AiModel.provider] existirem separados (sdd-006, B-07) — re-derivado do
 * [aiModel] salvo a cada leitura, sem precisar reescrever o DataStore.
 */
private fun resolveStoredAiProvider(storedProvider: String?, aiModel: String): String =
    when (storedProvider) {
        null, "gemini", "openrouter" -> storedProvider ?: "openrouter"
        else -> providerFor(aiModel)
    }

/**
 * Resolve o provider técnico (`"openrouter"`/`"gemini"`) de [model]: usa o catálogo quando o
 * modelo é conhecido; senão, cai no heurístico por nome (um modelo da família Gemini ainda pode
 * ser escolhido fora do catálogo atual, ex. via `aiModel` definido em outra tela).
 */
private fun providerFor(model: String): String =
    if (model.contains("gemini")) "gemini" else "openrouter"

internal class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
    private val reminderScheduler: ReminderScheduler
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences.toAppSettings() }

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
        reminderScheduler.sync(getSettings().first())
    }

    override suspend fun updateAiSettings(
        tier: AiConfigTier,
        model: String,
        creativity: Float,
        customApiKey: String?
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_CONFIG_TIER] = tier.name
            preferences[PreferencesKeys.AI_MODEL] = model
            preferences[PreferencesKeys.AI_PROVIDER] = providerFor(model)
            preferences[PreferencesKeys.CREATIVITY_LEVEL] = creativity

            if (customApiKey != null && customApiKey.isNotBlank()) {
                preferences[PreferencesKeys.CUSTOM_API_KEY] = customApiKey
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_API_KEY)
            }
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
        reminderScheduler.sync(getSettings().first())
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
