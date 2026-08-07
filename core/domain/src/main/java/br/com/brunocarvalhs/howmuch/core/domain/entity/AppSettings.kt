package br.com.brunocarvalhs.howmuch.core.domain.entity

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "18:00",
    val aiModel: String = "google/gemini-2.0-flash-001",
    val customPrompt: String? = null,
    val creativityLevel: Float = 0.7f,
    val defaultListId: String? = null,
    val sortingMode: String = "CATEGORY",
    val remindersEnabled: Boolean = false,
    val language: String = "pt",
    val currency: String = "BRL"
)
