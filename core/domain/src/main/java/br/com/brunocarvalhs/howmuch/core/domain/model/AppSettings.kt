package br.com.brunocarvalhs.howmuch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "18:00",
    val aiConfigTier: AiConfigTier = AiConfigTier.DEFAULT,
    val aiProvider: AiProvider = AiProvider.OPENROUTER,
    val aiModel: String = "openrouter/free",
    val customApiKey: String? = null,
    val creativityLevel: Float = 0.7f,
    val defaultListId: String? = null,
    val sortingMode: String = "CATEGORY",
    val remindersEnabled: Boolean = false,
    val language: String = "pt-BR",
    val currency: String = "BRL"
)
