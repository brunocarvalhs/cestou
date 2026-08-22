package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent

internal data class ShoppingSettingsIntent(
    val onUpdateShoppingPreferences: (String?, String, Boolean) -> Unit = { _, _, _ -> },
    val onBack: () -> Unit = {}
)
