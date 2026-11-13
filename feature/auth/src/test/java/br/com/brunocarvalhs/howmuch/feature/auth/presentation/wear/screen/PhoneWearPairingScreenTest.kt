package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.WearPairing
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PhoneWearPairingStage
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PhoneWearPairingUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel.PhoneWearPairingViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Layout tests (Robolectric, JVM) for the phone side of the watch pairing. Selectors are testTags.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PhoneWearPairingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val args = WearPairing(sessionId = "session-1", wearPublicKey = "key", wearNodeId = "node-1")

    private fun viewModel(state: PhoneWearPairingUiState) = mockk<PhoneWearPairingViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
    }

    @Test
    fun `connecting shows the progress without actions`() {
        composeTestRule.setContent {
            PhoneWearPairingScreen(
                viewModel = viewModel(PhoneWearPairingUiState(stage = PhoneWearPairingStage.CONNECTING)),
                args = args,
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_ROOT).assertIsDisplayed()
    }

    @Test
    fun `the code is shown and the phone user can confirm it`() {
        val vm = viewModel(PhoneWearPairingUiState(stage = PhoneWearPairingStage.CODE, code = "BDFG HKLM"))
        composeTestRule.setContent {
            PhoneWearPairingScreen(viewModel = vm, args = args, onClose = {})
        }

        composeTestRule.onNodeWithTag(TAG_CODE).assertContentDescriptionEquals("BDFG HKLM")
        composeTestRule.onNodeWithTag(TAG_CONFIRM).performClick()

        verify { vm.confirm(any()) }
    }

    @Test
    fun `after the phone confirmed it waits for the watch and cannot confirm again`() {
        composeTestRule.setContent {
            PhoneWearPairingScreen(
                viewModel = viewModel(
                    PhoneWearPairingUiState(
                        stage = PhoneWearPairingStage.CODE,
                        code = "BDFG HKLM",
                        isPhoneConfirmed = true
                    )
                ),
                args = args,
                onClose = {}
            )
        }

        composeTestRule.onAllNodesWithTag(TAG_CONFIRM).assertCountEquals(0)
        composeTestRule.onNodeWithTag(TAG_CANCEL).assertIsDisplayed()
    }

    @Test
    fun `cancel tells the view model and leaves the screen`() {
        val vm = viewModel(PhoneWearPairingUiState(stage = PhoneWearPairingStage.CODE, code = "BDFG HKLM"))
        var closed = false
        composeTestRule.setContent {
            PhoneWearPairingScreen(viewModel = vm, args = args, onClose = { closed = true })
        }

        composeTestRule.onNodeWithTag(TAG_CANCEL).performClick()

        verify { vm.cancel() }
        assertTrue(closed)
    }

    @Test
    fun `syncing shows the progress`() {
        composeTestRule.setContent {
            PhoneWearPairingScreen(
                viewModel = viewModel(PhoneWearPairingUiState(stage = PhoneWearPairingStage.SYNCING)),
                args = args,
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_ROOT).assertIsDisplayed()
    }

    @Test
    fun `done offers a way out`() {
        var closed = false
        composeTestRule.setContent {
            PhoneWearPairingScreen(
                viewModel = viewModel(PhoneWearPairingUiState(stage = PhoneWearPairingStage.DONE)),
                args = args,
                onClose = { closed = true }
            )
        }

        composeTestRule.onNodeWithTag(TAG_CLOSE).performClick()

        assertTrue(closed)
    }

    @Test
    fun `an error shows its reason and offers a way out`() {
        composeTestRule.setContent {
            PhoneWearPairingScreen(
                viewModel = viewModel(
                    PhoneWearPairingUiState(stage = PhoneWearPairingStage.ERROR, failure = PairingFailure.NOT_PRO)
                ),
                args = args,
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_CLOSE).assertIsDisplayed()
    }

    private companion object {
        const val TAG_ROOT = "phone_pair_root"
        const val TAG_CODE = "phone_pair_code"
        const val TAG_CONFIRM = "phone_pair_confirm"
        const val TAG_CANCEL = "phone_pair_cancel"
        const val TAG_CLOSE = "phone_pair_close"
        const val TAG_ERROR = "phone_pair_error"
    }
}
