package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearProViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WearProGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val contentTag = "wear_pro_gate_content"

    private fun viewModel(isPro: Boolean?) = mockk<WearProViewModel>(relaxed = true) {
        every { this@mockk.isPro } returns MutableStateFlow(isPro)
    }

    @Test
    fun `pro subscribers see the gated content`() {
        composeTestRule.setContent {
            WearProGate(viewModel = viewModel(isPro = true)) {
                Text("gated", modifier = Modifier.testTag(contentTag))
            }
        }

        composeTestRule.onNodeWithTag(contentTag).assertIsDisplayed()
    }

    @Test
    fun `free accounts see the subscription screen and can open the phone`() {
        val vm = viewModel(isPro = false)
        composeTestRule.setContent {
            WearProGate(viewModel = vm) { Text("gated") }
        }

        composeTestRule.onNodeWithTag(TAG_PRO_REQUIRED).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_PRO_OPEN_PHONE).performClick()

        verify { vm.openPhone() }
    }

    @Test
    fun `status not yet received from the phone shows the subscription screen`() {
        composeTestRule.setContent {
            WearProGate(viewModel = viewModel(isPro = null)) { Text("gated") }
        }

        composeTestRule.onNodeWithTag(TAG_PRO_REQUIRED).assertIsDisplayed()
    }
}
