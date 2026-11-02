package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.AiSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Split out of AiSettingsScreenTest (BYOK tier flow) to stay under Detekt's TooManyFunctions
// threshold once the two independently-written coverage suites for this screen were merged.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AiSettingsByokTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var saveCalls = 0
    private var backCalls = 0

    private fun string(resId: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resId)

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
    fun `selecting tier 3 as a pro user reveals the custom api key field`() {
        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(subscriptionStatus = SubscriptionStatus.PRO),
                    intent = AiSettingsIntent(),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.settings_ai_tier_3_title)).performScrollTo().performClick()

        composeTestRule.onNodeWithText(string(R.string.settings_ai_label_custom_api_key)).assertExists()
    }

    @Test
    fun `typing a custom api key updates the field`() {
        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(
                        aiConfigTier = AiConfigTier.CUSTOM_BYOK,
                        subscriptionStatus = SubscriptionStatus.PRO,
                    ),
                    intent = AiSettingsIntent(),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.settings_ai_label_custom_api_key))
            .performTextInput("sk-test-123")

        composeTestRule.onNodeWithText("sk-test-123").assertExists()
    }

    @Test
    fun `selecting tier 3 as a free user opens the paywall instead of switching tier`() {
        var paywallOpened = false

        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(subscriptionStatus = SubscriptionStatus.FREE),
                    intent = AiSettingsIntent(onOpenPaywall = { paywallOpened = true }),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.settings_ai_tier_3_title)).performScrollTo().performClick()

        assertEquals(true, paywallOpened)
    }
}
