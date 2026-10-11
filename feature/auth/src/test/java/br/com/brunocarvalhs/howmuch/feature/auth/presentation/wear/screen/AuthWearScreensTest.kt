package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel.PairingViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM) das telas Wear de pareamento.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AuthWearScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun viewModel(state: PairingUiState) = mockk<PairingViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
    }

    @Test
    fun `pairing screen shows the code`() {
        val navigator = mockk<Navigator>(relaxed = true)
        composeTestRule.setContent {
            PairingCodeWearScreen(viewModel = viewModel(PairingUiState(code = "123456")), navigator = navigator)
        }
        composeTestRule.onNodeWithText("123456", substring = true).assertExists()
    }

    @Test
    fun `pairing screen renders while the code is loading`() {
        composeTestRule.setContent {
            PairingCodeWearScreen(viewModel = viewModel(PairingUiState()), navigator = mockk(relaxed = true))
        }
        composeTestRule.onNodeWithText("Código de Pareamento").assertExists()
    }

    @Test
    fun `pairing screen navigates away once linked`() {
        val navigator = mockk<Navigator>(relaxed = true)
        composeTestRule.setContent {
            PairingCodeWearScreen(viewModel = viewModel(PairingUiState(isLinked = true)), navigator = navigator)
        }
        composeTestRule.waitForIdle()
        verify { navigator.navigate(any(), any()) }
    }

    @Test
    fun `link phone screen navigates to the pairing code`() {
        val navigator = mockk<Navigator>(relaxed = true)
        composeTestRule.setContent {
            LinkPhoneWearScreen(viewModel = viewModel(PairingUiState()), navigator = navigator)
        }
        composeTestRule.onNodeWithText("Vincular Celular").performClick()
        verify { navigator.navigate(any(), any()) }
    }
}
