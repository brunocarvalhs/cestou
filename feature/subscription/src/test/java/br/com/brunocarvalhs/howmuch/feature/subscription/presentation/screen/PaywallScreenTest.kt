package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.subscription.R
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.PaywallIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PaywallUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PaywallScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun context() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun string(id: Int) = context().getString(id)

    @Test
    fun `renders the formatted price on the subscribe button`() {
        composeTestRule.setContent {
            CestouTheme {
                PaywallScreen(
                    state = PaywallUiState(formattedPrice = "R$ 9,90", isLoadingPrice = false),
                    intent = PaywallIntent(),
                    activity = null
                )
            }
        }

        composeTestRule.onNodeWithText(context().getString(R.string.paywall_subscribe_button, "R$ 9,90"))
            .assertExists()
    }

    @Test
    fun `renders the already-Pro message when the user is Pro`() {
        composeTestRule.setContent {
            CestouTheme {
                PaywallScreen(
                    state = PaywallUiState(status = SubscriptionStatus.PRO),
                    intent = PaywallIntent(),
                    activity = null
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.paywall_already_pro)).assertExists()
    }
}
