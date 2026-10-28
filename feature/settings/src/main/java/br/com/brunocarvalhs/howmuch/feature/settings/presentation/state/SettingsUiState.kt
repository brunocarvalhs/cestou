package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

@Stable
data class SettingsUiState(
    val sections: List<SettingSection> = emptyList(),
    val expandedSectionIds: Set<String> = emptySet()
)

@Stable
data class SettingSection(
    val id: String,
    val title: UiText,
    val icon: ImageVector,
    val items: List<SettingItem>
)

@Stable
data class SettingItem(
    val title: UiText,
    val subtitle: UiText? = null,
    val icon: ImageVector,
    val route: Any? = null
)
