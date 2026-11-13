package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import br.com.brunocarvalhs.howmuch.feature.shopping.R

/** Tags dos botões do menu, para os testes de UI (AGENTS §5: sem asserts de texto). */
internal const val TAG_MENU_NOTIFICATIONS = "wear_menu_notifications"
internal const val TAG_MENU_AI = "wear_menu_ai"
internal const val TAG_MENU_HISTORY = "wear_menu_history"
internal const val TAG_MENU_SETTINGS = "wear_menu_settings"

/**
 * Menu do relógio: reúne as funcionalidades secundárias, abertas pelo ícone de
 * menu da home. A entrada de IA só aparece com [showAi] (flag `wear_ai_quick_entry`).
 */
@Composable
internal fun WearMenuScreen(
    showAi: Boolean,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_menu))
                }
            }

            menuItem(
                spec = spec,
                tag = TAG_MENU_NOTIFICATIONS,
                label = R.string.shopping_wear_notifications_title,
                onClick = onNavigateToNotifications
            )
            if (showAi) {
                menuItem(
                    spec = spec,
                    tag = TAG_MENU_AI,
                    label = R.string.shopping_wear_menu_ai,
                    onClick = onNavigateToAi
                )
            }
            menuItem(
                spec = spec,
                tag = TAG_MENU_HISTORY,
                label = R.string.shopping_wear_home_history,
                onClick = onNavigateToHistory
            )
            menuItem(
                spec = spec,
                tag = TAG_MENU_SETTINGS,
                label = R.string.shopping_wear_settings,
                onClick = onNavigateToSettings
            )
        }
    }
}

/** Um botão do menu, com a mesma cor de card da home. */
private fun TransformingLazyColumnScope.menuItem(
    spec: TransformationSpec,
    tag: String,
    label: Int,
    onClick: () -> Unit
) {
    item {
        WearMenuButton(spec = spec, tag = tag, label = label, onClick = onClick)
    }
}

@Composable
private fun TransformingLazyColumnItemScope.WearMenuButton(
    spec: TransformationSpec,
    tag: String,
    label: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, spec)
            .testTag(tag),
        colors = wearCardColors(),
        transformation = SurfaceTransformation(spec)
    ) {
        Text(stringResource(label))
    }
}
