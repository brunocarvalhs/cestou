package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state.WelcomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
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
}
