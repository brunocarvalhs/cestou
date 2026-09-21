package br.com.brunocarvalhs.howmuch.feature.profile.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.profile.R
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the profile title and the current user's display name`() {
        val title = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.profile_title)
        val state = ProfileUiState(user = AuthenticatedUser(id = "u1", displayName = "Ana"))

        composeTestRule.setContent {
            CestouTheme {
                ProfileScreen(state = state, intent = ProfileIntent())
            }
        }

        composeTestRule.onNodeWithText(title).assertExists()
        composeTestRule.onNodeWithText("Ana").assertExists()
    }
}
