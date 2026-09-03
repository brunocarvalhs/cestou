package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingItem
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingSection
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingsUiState

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    intent: SettingsIntent
) {
    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_title),
                onBack = { intent.onBack() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            state.sections.forEach { section ->
                item {
                    SettingsSection(section.title.asString())
                }

                items(section.items) { item ->
                    SettingsItem(
                        title = item.title.asString(),
                        subtitle = item.subtitle?.asString(),
                        icon = item.icon,
                        onClick = {
                            item.route?.let { intent.onNavigate(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 24.dp,
            bottom = 8.dp
        ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(text = title)
        },
        supportingContent = subtitle?.let {
            {
                Text(text = it)
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Preview(showBackground = true, name = "Vazio")
@Composable
private fun SettingsEmptyPreview() {
    SettingsScreen(
        state = SettingsUiState(),
        intent = SettingsIntent()
    )
}

@Preview(showBackground = true, name = "Com seções")
@Composable
private fun SettingsPreview() {
    SettingsScreen(
        state = SettingsUiState(
            sections = listOf(
                SettingSection(
                    title = UiText.DynamicString("Preferências"),
                    items = listOf(
                        SettingItem(
                            title = UiText.DynamicString("Compras"),
                            subtitle = UiText.DynamicString("Ordenação e lembretes"),
                            icon = Icons.Default.ShoppingCart
                        ),
                        SettingItem(
                            title = UiText.DynamicString("Notificações"),
                            icon = Icons.Default.Notifications
                        ),
                        SettingItem(
                            title = UiText.DynamicString("Assistente de IA"),
                            subtitle = UiText.DynamicString("Modelo e criatividade"),
                            icon = Icons.Default.SmartToy
                        )
                    )
                ),
                SettingSection(
                    title = UiText.DynamicString("Sobre"),
                    items = listOf(
                        SettingItem(
                            title = UiText.DynamicString("Novidades"),
                            icon = Icons.Default.Info
                        )
                    )
                )
            )
        ),
        intent = SettingsIntent()
    )
}
