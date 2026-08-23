package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class ShoppingSettingsUiState(
    val defaultListId: String? = null,
    val sortingMode: String = "CATEGORY",
    val remindersEnabled: Boolean = false
)
