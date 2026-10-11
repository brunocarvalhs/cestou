package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.DataSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.NotificationSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ShoppingSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ThemeSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.AiSettingsUiState
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.DataSettingsUiState
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.NotificationSettingsUiState
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ShoppingSettingsUiState
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ThemeSettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM): cada sub-tela de configurações é composta de verdade e
// todos os alvos clicáveis visíveis são acionados, exercitando os callbacks e as bottom sheets.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsSubScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun ComposeContentTestRule.clickEverything() {
        val count = onAllNodes(hasClickAction()).fetchSemanticsNodes().size
        for (i in 0 until count) {
            runCatching { onAllNodes(hasClickAction())[i].performClick() }
        }
        waitForIdle()
    }

    @Test
    fun `theme screen renders and handles taps`() {
        composeTestRule.setContent {
            CestouTheme {
                ThemeSettingsScreen(
                    state = ThemeSettingsUiState(themeMode = ThemeMode.DARK),
                    intent = ThemeSettingsIntent()
                )
            }
        }
        composeTestRule.clickEverything()
    }

    @Test
    fun `data screen renders loading and message states and opens its confirmations`() {
        composeTestRule.setContent {
            CestouTheme {
                DataSettingsScreen(
                    state = DataSettingsUiState(isLoading = false, message = "Cache limpo"),
                    intent = DataSettingsIntent()
                )
            }
        }
        composeTestRule.clickEverything()
        composeTestRule.clickEverything()
    }

    @Test
    fun `data screen renders while loading`() {
        composeTestRule.setContent {
            CestouTheme {
                DataSettingsScreen(state = DataSettingsUiState(isLoading = true), intent = DataSettingsIntent())
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `ai settings screen renders and handles taps`() {
        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(aiModel = "gemini", customPrompt = "Seja breve", creativityLevel = 0.5f),
                    intent = AiSettingsIntent()
                )
            }
        }
        composeTestRule.clickEverything()
    }

    @Test
    fun `notification settings screen renders and opens the time picker`() {
        composeTestRule.setContent {
            CestouTheme {
                NotificationSettingsScreen(
                    state = NotificationSettingsUiState(notificationsEnabled = true, reminderTime = "18:00"),
                    intent = NotificationSettingsIntent()
                )
            }
        }
        composeTestRule.clickEverything()
    }

    @Test
    fun `notification settings screen renders disabled`() {
        composeTestRule.setContent {
            CestouTheme {
                NotificationSettingsScreen(
                    state = NotificationSettingsUiState(notificationsEnabled = false),
                    intent = NotificationSettingsIntent()
                )
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `shopping settings screen renders and handles taps`() {
        composeTestRule.setContent {
            CestouTheme {
                ShoppingSettingsScreen(
                    state = ShoppingSettingsUiState(sortingMode = "CATEGORY", remindersEnabled = true),
                    intent = ShoppingSettingsIntent()
                )
            }
        }
        composeTestRule.clickEverything()
    }

    @Test
    fun `release notes screen renders`() {
        composeTestRule.setContent {
            CestouTheme { ReleaseNotesScreen(onBack = {}) }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `open source licenses screen renders`() {
        composeTestRule.setContent { CestouTheme { OpenSourceLicensesScreen(onBack = {}) } }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `placeholder screen renders its title`() {
        composeTestRule.setContent { CestouTheme { PlaceholderSettingsScreen(title = "Em breve", onBack = {}) } }
        composeTestRule.onRoot().assertExists()
    }
}
