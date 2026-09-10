package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CustomMethodPickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the full consent sentence with both links`() {
        composeTestRule.setContent {
            CestouTheme {
                CustomMethodPickerTerms()
            }
        }

        composeTestRule.onNodeWithText("Termos de Uso", substring = true).assertExists()
        composeTestRule.onNodeWithText("Política de Privacidade", substring = true).assertExists()
    }

    @Test
    fun `clicking Termos de Uso triggers the terms of use callback`() {
        var termsClicked = false
        var privacyClicked = false

        composeTestRule.setContent {
            CestouTheme {
                CustomMethodPickerTerms(
                    onOpenTermsOfUse = { termsClicked = true },
                    onOpenPrivacyPolicy = { privacyClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Termos de Uso").performClick()

        assert(termsClicked)
        assert(!privacyClicked)
    }

    @Test
    fun `clicking Politica de Privacidade triggers the privacy policy callback`() {
        var termsClicked = false
        var privacyClicked = false

        composeTestRule.setContent {
            CestouTheme {
                CustomMethodPickerTerms(
                    onOpenTermsOfUse = { termsClicked = true },
                    onOpenPrivacyPolicy = { privacyClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Política de Privacidade").performClick()

        assert(privacyClicked)
        assert(!termsClicked)
    }
}
