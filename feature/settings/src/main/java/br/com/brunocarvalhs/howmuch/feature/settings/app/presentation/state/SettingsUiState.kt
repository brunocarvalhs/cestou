package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state

import androidx.compose.ui.graphics.vector.ImageVector
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

internal data class SettingsUiState(
    val sections: List<SettingSection> = emptyList()
)

internal data class SettingSection(
    val title: UiText,
    val items: List<SettingItem>
)

internal data class SettingItem(
    val title: UiText,
    val subtitle: UiText? = null,
    val icon: ImageVector,
    val route: Any? = null
)
