package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.subscription.R
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.PaywallIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PaywallUiState
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PlanUi
import org.junit.Assert.assertEquals
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
                    state = PaywallUiState(
                        plans = listOf(PlanUi("monthly", "R$ 9,90")),
                        selectedPlanId = "monthly",
                        isLoadingPlans = false
                    ),
                    intent = PaywallIntent(),
                    activity = null
                )
            }
        }

        composeTestRule.onNodeWithText(context().getString(R.string.paywall_subscribe_button, "R$ 9,90"))
            .assertExists()
    }

    @Test
    fun `renders one card per plan and reports the tapped plan`() {
        var tappedPlanId: String? = null
        composeTestRule.setContent {
            CestouTheme {
                PaywallScreen(
                    state = PaywallUiState(
                        plans = listOf(PlanUi("monthly", "R$ 9,90"), PlanUi("annual", "R$ 99,90")),
                        selectedPlanId = "annual",
                        isLoadingPlans = false
                    ),
                    intent = PaywallIntent(onPlanSelected = { tappedPlanId = it }),
                    activity = null
                )
            }
        }

        composeTestRule.onNodeWithTag("paywall_plan_annual").assertExists()
        composeTestRule.onNodeWithTag("paywall_plan_monthly").performClick()

        assertEquals("monthly", tappedPlanId)
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

    @Test
    fun `renders the Pro benefits list for a Free user`() {
        composeTestRule.setContent {
            CestouTheme {
                PaywallScreen(
                    state = PaywallUiState(
                        plans = listOf(PlanUi("monthly", "R$ 9,90")),
                        selectedPlanId = "monthly",
                        isLoadingPlans = false
                    ),
                    intent = PaywallIntent(),
                    activity = null
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.paywall_benefit_chat)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.paywall_benefit_share)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.paywall_benefit_qr_code)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.paywall_benefit_wear)).assertExists()
    }

    @Test
    fun `hides the Pro benefits list for a Pro user`() {
        composeTestRule.setContent {
            CestouTheme {
                PaywallScreen(
                    state = PaywallUiState(status = SubscriptionStatus.PRO),
                    intent = PaywallIntent(),
                    activity = null
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.paywall_benefit_chat)).assertDoesNotExist()
    }
}
