package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.AiSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// sdd-017, B-15: tier=CUSTOM_BYOK with a blank key must never silently save.
@RunWith(AndroidJUnit4::class)
class AiSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var saveCalls = 0
    private var backCalls = 0

    private fun launch(customApiKey: String = "") = composeTestRule.setContent {
        CestouTheme {
            AiSettingsScreen(
                state = AiSettingsUiState(
                    aiConfigTier = AiConfigTier.CUSTOM_BYOK,
                    customApiKey = customApiKey,
                    subscriptionStatus = SubscriptionStatus.PRO
                ),
                intent = AiSettingsIntent(
                    onUpdateAiSettings = { _, _, _, _ -> saveCalls++ },
                    onBack = { backCalls++ }
                )
            )
        }
    }

    @Test
    fun savingWithBlankByokKeyShowsErrorAndDoesNotSave() {
        launch(customApiKey = "")

        composeTestRule.onNodeWithTag("ai_settings_save_button").performClick()

        composeTestRule.onNodeWithTag("ai_settings_byok_key_error").assertExists()
        assertEquals(0, saveCalls)
        assertEquals(0, backCalls)
    }

    @Test
    fun fillingByokKeyAfterErrorAllowsSaving() {
        launch(customApiKey = "")

        composeTestRule.onNodeWithTag("ai_settings_save_button").performClick()
        composeTestRule.onNodeWithTag("ai_settings_byok_key_error").assertExists()

        composeTestRule.onNodeWithTag("ai_settings_byok_key_field").performTextInput("sk-test-key")
        composeTestRule.onNodeWithTag("ai_settings_save_button").performClick()

        assertEquals(1, saveCalls)
        assertEquals(1, backCalls)
    }

    @Test
    fun savingWithNonBlankByokKeySavesImmediately() {
        launch(customApiKey = "sk-already-set")

        composeTestRule.onNodeWithTag("ai_settings_save_button").performClick()

        composeTestRule.onNodeWithTag("ai_settings_byok_key_error").assertDoesNotExist()
        assertEquals(1, saveCalls)
        assertEquals(1, backCalls)
    }
}
