package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

internal data class ShoppingSettingsUiState(
    val defaultListId: String? = null,
    val sortingMode: String = "CATEGORY",
    val remindersEnabled: Boolean = false
)
