package br.com.brunocarvalhs.howmuch.feature.profile.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
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

    @Test
    fun `tapping the settings icon triggers onNavigate`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var navigated = false

        composeTestRule.setContent {
            CestouTheme {
                ProfileScreen(
                    state = ProfileUiState(),
                    intent = ProfileIntent(onNavigate = { navigated = true })
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.profile_settings_content_description))
            .performClick()
        assert(navigated)
    }

    @Test
    fun `a Free user sees the upgrade card and can tap it`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var tapped = false

        composeTestRule.setContent {
            CestouTheme {
                ProfileScreen(
                    state = ProfileUiState(),
                    intent = ProfileIntent(onManageSubscription = { tapped = true }),
                    subscriptionStatus = SubscriptionStatus.FREE
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_subscription_free_title)).performClick()
        assert(tapped)
    }

    @Test
    fun `a Pro user sees the active subscription badge`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeTestRule.setContent {
            CestouTheme {
                ProfileScreen(
                    state = ProfileUiState(),
                    intent = ProfileIntent(),
                    subscriptionStatus = SubscriptionStatus.PRO
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_subscription_pro_subtitle)).assertExists()
    }
}
