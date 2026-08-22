package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent

internal data class AiSettingsIntent(
    val onUpdateAiSettings: (String, String?, Float) -> Unit = { _, _, _ -> },
    val onBack: () -> Unit = {}
)
