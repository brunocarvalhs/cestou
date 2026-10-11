package br.com.brunocarvalhs.howmuch.feature.profile.presentation.wear.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.feature.profile.R
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM) da tela Wear de perfil.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ProfileWearScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun viewModel(state: ProfileUiState) = mockk<ProfileViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
    }

    @Test
    fun `renders the signed-in user with the account actions`() {
        val state = ProfileUiState(
            user = AuthenticatedUser(id = "1", email = "bruno@example.com", displayName = "Bruno Carvalho")
        )
        composeTestRule.setContent { ProfileWearScreen(viewModel = viewModel(state)) }
        composeTestRule.onNodeWithText(string(R.string.profile_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.profile_menu_sign_out)).assertExists()
    }

    @Test
    fun `renders without a signed-in user`() {
        composeTestRule.setContent { ProfileWearScreen(viewModel = viewModel(ProfileUiState())) }
        composeTestRule.onNodeWithText(string(R.string.profile_title)).assertExists()
    }
}
