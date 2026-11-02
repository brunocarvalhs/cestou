package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingItem
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingSection

/**
 * Non-exclusive accordion: each [SettingSection] expands/collapses independently, so more than
 * one can stay open at once. Uses a plain [Column] (not [androidx.compose.foundation.lazy.LazyColumn])
 * because the caller (`feature:profile`'s Profile screen) already hosts this inside its own
 * scrollable Column, and a lazy list nested in a scrollable one crashes on infinite constraints.
 */
@Composable
fun SettingsSectionsList(
    sections: List<SettingSection>,
    expandedSectionIds: Set<String>,
    intent: SettingsIntent,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        sections.forEach { section ->
            key(section.id) {
                SettingsAccordionSection(
                    section = section,
                    expanded = section.id in expandedSectionIds,
                    onToggleSection = intent.onToggleSection,
                    onItemClick = { item -> item.route?.let { intent.onNavigate(it) } }
                )
            }
        }
    }
}

@Composable
private fun SettingsAccordionSection(
    section: SettingSection,
    expanded: Boolean,
    onToggleSection: (String) -> Unit,
    onItemClick: (SettingItem) -> Unit
) {
    Column {
        SettingsSectionHeader(
            title = section.title.asString(),
            icon = section.icon,
            expanded = expanded,
            onClick = { onToggleSection(section.id) },
            modifier = Modifier.testTag("settings_section_header_${section.id}")
        )
        HorizontalDivider()

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.testTag("settings_section_content_${section.id}")) {
                section.items.forEachIndexed { index, item ->
                    SettingsItem(
                        title = item.title.asString(),
                        subtitle = item.subtitle?.asString(),
                        icon = item.icon,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.testTag("settings_item_${section.id}_$index")
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "sectionChevron")

    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics {
                if (expanded) {
                    collapse {
                        onClick()
                        true
                    }
                } else {
                    expand {
                        onClick()
                        true
                    }
                }
            },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation)
            )
        }
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
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

@PreviewCestouScreens
@Composable
private fun SettingsSectionsListEmptyPreview() {
    CestouTheme {
        SettingsSectionsList(
            sections = emptyList(),
            expandedSectionIds = emptySet(),
            intent = SettingsIntent(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
    }
}

@PreviewCestouScreens
@Composable
private fun SettingsSectionsListPreview() {
    val sections = listOf(
        SettingSection(
            id = "general",
            title = UiText.DynamicString("Preferências"),
            icon = Icons.Default.ShoppingCart,
            items = listOf(
                SettingItem(
                    title = UiText.DynamicString("Compras"),
                    subtitle = UiText.DynamicString("Ordenação e lembretes"),
                    icon = Icons.Default.ShoppingCart
                ),
                SettingItem(
                    title = UiText.DynamicString("Notificações"),
                    icon = Icons.Default.Notifications
                )
            )
        ),
        SettingSection(
            id = "about",
            title = UiText.DynamicString("Sobre"),
            icon = Icons.Default.Info,
            items = listOf(
                SettingItem(
                    title = UiText.DynamicString("Novidades"),
                    icon = Icons.Default.Info
                )
            )
        )
    )
    CestouTheme {
        SettingsSectionsList(
            sections = sections,
            expandedSectionIds = setOf("general"),
            intent = SettingsIntent(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
    }
}
