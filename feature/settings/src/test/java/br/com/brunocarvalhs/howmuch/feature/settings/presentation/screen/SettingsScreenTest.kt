package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingItem
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingSection
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders every section title and item title`() {
        val state = SettingsUiState(
            sections = listOf(
                SettingSection(
                    title = UiText.DynamicString("General"),
                    items = listOf(
                        SettingItem(title = UiText.DynamicString("Theme"), icon = Icons.Default.Info)
                    )
                )
            )
        )

        composeTestRule.setContent {
            CestouTheme {
                SettingsScreen(state = state, intent = SettingsIntent())
            }
        }

        composeTestRule.onNodeWithText("General").assertExists()
        composeTestRule.onNodeWithText("Theme").assertExists()
    }
}
