package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui.GoogleAuthProviderFactory
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

        composeTestRule.onNodeWithText("Terms of Use", substring = true).assertExists()
        composeTestRule.onNodeWithText("Privacy Policy", substring = true).assertExists()
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

        composeTestRule.onNodeWithText("Terms of Use").performClick()

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

        composeTestRule.onNodeWithText("Privacy Policy").performClick()

        assert(privacyClicked)
        assert(!termsClicked)
    }

    @Test
    fun `method picker lists Google then e-mail, with no phone option in phase 1`() {
        var emailSelected = false
        composeTestRule.setContent {
            CestouTheme {
                CustomMethodPickerLayout(
                    providers = listOf(GoogleAuthProviderFactory()()),
                    onProviderSelected = {},
                    onEmailSelected = { emailSelected = true }
                )
            }
        }

        val google = composeTestRule.onNodeWithTag("welcome_google_button").fetchSemanticsNode().boundsInRoot
        val email = composeTestRule.onNodeWithTag("welcome_email_button").fetchSemanticsNode().boundsInRoot
        assert(google.top < email.top)
        composeTestRule.onNodeWithTag("welcome_phone_button").assertDoesNotExist()

        composeTestRule.onNodeWithTag("welcome_email_button").performClick()
        assert(emailSelected)
    }
}
