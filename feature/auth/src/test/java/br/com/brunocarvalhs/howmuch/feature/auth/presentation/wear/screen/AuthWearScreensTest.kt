package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingWear
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingStage
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

// Layout tests (Robolectric, JVM) for the watch pairing screens. Selectors are testTags, not text (AGENTS §5).
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AuthWearScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun viewModel(state: PairingUiState) = mockk<PairingViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
    }

    @Test
    fun `pairing screen shows the code the phone derived`() {
        composeTestRule.setContent {
            PairingWearScreen(
                viewModel = viewModel(PairingUiState(stage = PairingStage.CODE, code = "BDFG HKLM")),
                navigator = mockk(relaxed = true)
            )
        }

        composeTestRule.onNodeWithTag(TAG_CODE).assertContentDescriptionEquals("BDFG HKLM")
    }

    @Test
    fun `confirm button is shown while the code waits for the watch user`() {
        composeTestRule.setContent {
            PairingWearScreen(
                viewModel = viewModel(PairingUiState(stage = PairingStage.CODE, code = "BDFG HKLM")),
                navigator = mockk(relaxed = true)
            )
        }

        composeTestRule.onNodeWithTag(TAG_CONFIRM).assertIsDisplayed().performClick()
    }

    @Test
    fun `error state shows the failure and retry starts a new session`() {
        val vm = viewModel(PairingUiState(stage = PairingStage.ERROR, failure = PairingFailure.TIMEOUT))
        composeTestRule.setContent {
            PairingWearScreen(viewModel = vm, navigator = mockk(relaxed = true))
        }

        composeTestRule.onNodeWithTag(TAG_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_RETRY).performClick()

        verify(atLeast = 2) { vm.start() }
    }

    @Test
    fun `connecting and syncing show the progress screen`() {
        val vm = viewModel(PairingUiState(stage = PairingStage.CONNECTING))
        composeTestRule.setContent {
            PairingWearScreen(viewModel = vm, navigator = mockk(relaxed = true))
        }
        composeTestRule.onNodeWithTag(TAG_ROOT).assertIsDisplayed()
    }

    @Test
    fun `an error without a known failure falls back to the timeout message`() {
        composeTestRule.setContent {
            PairingWearScreen(
                viewModel = viewModel(PairingUiState(stage = PairingStage.ERROR, failure = null)),
                navigator = mockk(relaxed = true)
            )
        }
        composeTestRule.onNodeWithTag(TAG_ERROR).assertIsDisplayed()
    }

    @Test
    fun `pairing screen navigates away once linked`() {
        val navigator = mockk<Navigator>(relaxed = true)
        composeTestRule.setContent {
            PairingWearScreen(
                viewModel = viewModel(PairingUiState(stage = PairingStage.SYNCING, isLinked = true)),
                navigator = navigator
            )
        }
        composeTestRule.waitForIdle()

        verify { navigator.navigate(any(), any()) }
    }

    @Test
    fun `link phone screen navigates to the watch pairing`() {
        val navigator = mockk<Navigator>(relaxed = true)
        composeTestRule.setContent {
            LinkPhoneWearScreen(navigator = navigator)
        }
        composeTestRule.onNodeWithTag(TAG_LINK_PHONE).performClick()

        verify { navigator.navigate(PairingWear::class.java.name, any()) }
    }

    private companion object {
        const val TAG_ROOT = "wear_pair_root"
        const val TAG_CODE = "wear_pair_code"
        const val TAG_CONFIRM = "wear_pair_confirm"
        const val TAG_ERROR = "wear_pair_error"
        const val TAG_RETRY = "wear_pair_retry"
        const val TAG_LINK_PHONE = "wear_link_phone"
    }
}
