package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

@Stable
internal data class SettingsUiState(
    val sections: List<SettingSection> = emptyList()
)

@Stable
internal data class SettingSection(
    val title: UiText,
    val items: List<SettingItem>
)

@Stable
internal data class SettingItem(
    val title: UiText,
    val subtitle: UiText? = null,
    val icon: ImageVector,
    val route: Any? = null
)
