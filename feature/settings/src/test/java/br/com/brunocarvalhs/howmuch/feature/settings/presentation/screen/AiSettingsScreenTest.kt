package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
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
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AiSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resId)

    @Test
    fun `shows a pro badge on tier 2 and 3 for a free subscription`() {
        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(subscriptionStatus = SubscriptionStatus.FREE),
                    intent = AiSettingsIntent(),
                )
            }
        }

        composeTestRule.onAllNodesWithText(string(R.string.settings_ai_pro_badge)).assertCountEquals(2)
    }

    @Test
    fun `selecting tier 2 as a free user opens the paywall instead of switching tier`() {
        var paywallOpened = false

        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(subscriptionStatus = SubscriptionStatus.FREE),
                    intent = AiSettingsIntent(onOpenPaywall = { paywallOpened = true }),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.settings_ai_tier_2_title)).performClick()

        assertEquals(true, paywallOpened)
        composeTestRule.onNodeWithText(string(R.string.settings_ai_label_model)).assertDoesNotExist()
    }

    @Test
    fun `selecting tier 2 as a pro user reveals the model dropdown`() {
        val model = AiModel(id = "openrouter/free", name = "OpenRouter Free", vendor = "OpenRouter")

        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(
                        subscriptionStatus = SubscriptionStatus.PRO,
                        availableModels = listOf(model),
                    ),
                    intent = AiSettingsIntent(),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.settings_ai_tier_2_title)).performClick()

        composeTestRule.onNodeWithText(string(R.string.settings_ai_label_model)).assertExists()
    }

    @Test
    fun `picking a model from the dropdown updates the displayed value`() {
        val freeModel = AiModel(id = "openrouter/free", name = "OpenRouter Free", vendor = "OpenRouter")
        val paidModel = AiModel(id = "openrouter/paid", name = "OpenRouter Paid", vendor = "OpenRouter")

        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(
                        aiConfigTier = AiConfigTier.SPECIFIC,
                        aiModel = freeModel.id,
                        subscriptionStatus = SubscriptionStatus.PRO,
                        availableModels = listOf(freeModel, paidModel),
                    ),
                    intent = AiSettingsIntent(),
                )
            }
        }

        composeTestRule.onNodeWithText(freeModel.name).performClick()
        composeTestRule.onNodeWithText(paidModel.name).performClick()

        composeTestRule.onNodeWithText(paidModel.name).assertExists()
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

    @Test
    fun `tapping save reports the current settings and navigates back`() {
        var reportedTier: AiConfigTier? = null
        var backCalled = false

        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(aiConfigTier = AiConfigTier.DEFAULT),
                    intent = AiSettingsIntent(
                        onUpdateAiSettings = { tier, _, _, _ -> reportedTier = tier },
                        onBack = { backCalled = true },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.settings_ai_button_save)).performScrollTo().performClick()

        assertEquals(AiConfigTier.DEFAULT, reportedTier)
        assertEquals(true, backCalled)
    }

    @Test
    fun `tapping the header back arrow also reports the current settings`() {
        var backCalled = false

        composeTestRule.setContent {
            CestouTheme {
                AiSettingsScreen(
                    state = AiSettingsUiState(),
                    intent = AiSettingsIntent(onBack = { backCalled = true }),
                )
            }
        }

        val backDescription = string(CoreUiR.string.content_description_back)
        composeTestRule.onNodeWithContentDescription(backDescription).performClick()

        assertEquals(true, backCalled)
    }
}
