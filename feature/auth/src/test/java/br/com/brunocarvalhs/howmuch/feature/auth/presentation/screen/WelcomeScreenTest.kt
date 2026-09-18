package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.WelcomeIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.AuthErrorType
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.WelcomeUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the app logo name and a sign-in action`() {
        composeTestRule.setContent {
            CestouTheme {
                WelcomeScreen(state = WelcomeUiState(version = "1.3.0")) {
                    Text("Entrar")
                }
            }
        }

        composeTestRule.onNodeWithText("Cestou").assertExists()
        composeTestRule.onNodeWithText("Entrar").assertExists()
    }

    @Test
    fun `does not show the error sheet when there is no error`() {
        composeTestRule.setContent {
            CestouTheme {
                WelcomeScreen(state = WelcomeUiState()) {
                    Text("Entrar")
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_error_sheet").assertDoesNotExist()
    }

    @Test
    fun `shows the error sheet with the classified error copy`() {
        composeTestRule.setContent {
            CestouTheme {
                WelcomeScreen(state = WelcomeUiState(error = AuthErrorType.NO_CONNECTIVITY)) {
                    Text("Entrar")
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_error_sheet_title").assertExists()
        composeTestRule.onNodeWithTag("auth_error_sheet_message").assertExists()
    }

    @Test
    fun `dismissing the error sheet invokes onDismissError`() {
        var dismissed = false
        composeTestRule.setContent {
            CestouTheme {
                WelcomeScreen(
                    state = WelcomeUiState(error = AuthErrorType.STRUCTURAL),
                    intent = WelcomeIntent(onDismissError = { dismissed = true })
                ) {
                    Text("Entrar")
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_error_sheet_action_button").performClick()

        assertTrue(dismissed)
    }
}
