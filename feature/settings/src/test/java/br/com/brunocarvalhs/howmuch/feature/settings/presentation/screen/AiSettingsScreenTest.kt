package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.AiSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Espelha feature/settings/src/androidTest/.../AiSettingsScreenTest.kt em Robolectric (JVM) para
// contar na variante "layout" do Kover, já que testes androidTest não são medidos por ela.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
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

    // performScrollTo() antes de cada clique no botão salvar: a Column raiz tem
    // verticalScroll e, sob Robolectric, o viewport padrão é menor que o conteúdo —
    // sem rolar até o nó, o toque é descartado silenciosamente (sem exceção, mas sem
    // disparar o onClick), diferente do dispositivo real usado pelo teste androidTest.
    @Test
    fun `saving with blank byok key shows error and does not save`() {
        launch(customApiKey = "")

        composeTestRule.onNodeWithTag("ai_settings_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ai_settings_byok_key_error", useUnmergedTree = true).assertExists()
        assertEquals(0, saveCalls)
        assertEquals(0, backCalls)
    }

    @Test
    fun `filling byok key after error allows saving`() {
        launch(customApiKey = "")

        composeTestRule.onNodeWithTag("ai_settings_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_settings_byok_key_error", useUnmergedTree = true).assertExists()

        composeTestRule.onNodeWithTag("ai_settings_byok_key_field").performTextInput("sk-test-key")
        composeTestRule.onNodeWithTag("ai_settings_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, saveCalls)
        assertEquals(1, backCalls)
    }

    @Test
    fun `saving with non blank byok key saves immediately`() {
        launch(customApiKey = "sk-already-set")

        composeTestRule.onNodeWithTag("ai_settings_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ai_settings_byok_key_error", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(1, saveCalls)
        assertEquals(1, backCalls)
    }

    @Test
    fun `selecting the specific tier shows the model picker for pro users`() {
        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(subscriptionStatus = SubscriptionStatus.PRO),
                    intent = AiSettingsIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("ai_settings_byok_key_field").assertDoesNotExist()
    }

    @Test
    fun `specific tier model dropdown lists and selects an available model`() {
        var selectedModelId: String? = null

        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(
                        aiConfigTier = AiConfigTier.SPECIFIC,
                        aiModel = "openrouter/free",
                        availableModels = listOf(
                            AiModel("openrouter/free", "OpenRouter Free Router", vendor = "OpenRouter"),
                            AiModel("gemini/pro", "Gemini Pro", vendor = "Google")
                        ),
                        subscriptionStatus = SubscriptionStatus.PRO
                    ),
                    intent = AiSettingsIntent(
                        onUpdateAiSettings = { _, model, _, _ -> selectedModelId = model }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("OpenRouter Free Router").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Gemini Pro").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("ai_settings_save_button").performScrollTo().performClick()

        assertEquals("gemini/pro", selectedModelId)
    }
}
