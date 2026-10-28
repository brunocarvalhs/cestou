package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.ManageSubscriptionIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.ManageSubscriptionUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ManageSubscriptionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the formatted price once loaded`() {
        composeTestRule.setContent {
            MaterialTheme {
                ManageSubscriptionScreen(
                    state = ManageSubscriptionUiState(formattedPrice = "R$ 9,90", isLoadingPrice = false),
                    intent = ManageSubscriptionIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("manage_subscription_price_text").assertExists()
    }

    @Test
    fun `tapping the manage button delegates to onManageOnPlayStore`() {
        var opened = false
        composeTestRule.setContent {
            MaterialTheme {
                ManageSubscriptionScreen(
                    state = ManageSubscriptionUiState(formattedPrice = "R$ 9,90", isLoadingPrice = false),
                    intent = ManageSubscriptionIntent(onManageOnPlayStore = { opened = true })
                )
            }
        }

        composeTestRule.onNodeWithTag("manage_subscription_manage_button").performClick()

        assert(opened)
    }

    @Test
    fun `tapping back delegates to onBack`() {
        var backPressed = false
        composeTestRule.setContent {
            MaterialTheme {
                ManageSubscriptionScreen(
                    state = ManageSubscriptionUiState(formattedPrice = "R$ 9,90", isLoadingPrice = false),
                    intent = ManageSubscriptionIntent(onBack = { backPressed = true })
                )
            }
        }

        composeTestRule.onNodeWithTag("manage_subscription_back_button").performClick()

        assert(backPressed)
    }
}
