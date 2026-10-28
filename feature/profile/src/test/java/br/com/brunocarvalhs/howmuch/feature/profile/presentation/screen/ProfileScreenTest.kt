package br.com.brunocarvalhs.howmuch.feature.profile.presentation.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.profile.R
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingItem
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingSection
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingsUiState
import org.junit.Assert.assertEquals
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
    fun `renders the current user's display name and email in the header`() {
        val state = ProfileUiState(user = AuthenticatedUser(id = "u1", displayName = "Ana", email = "ana@example.com"))

        composeTestRule.setContent {
            CestouTheme {
                ProfileScreen(state = state, intent = ProfileIntent())
            }
        }

        composeTestRule.onNodeWithText("Ana").assertExists()
        composeTestRule.onNodeWithText("ana@example.com").assertExists()
    }

    @Test
    fun `tapping an already expanded settings leaf item delegates to settingsIntent onNavigate`() {
        var navigatedTo: Any? = null
        val settingsState = SettingsUiState(
            sections = listOf(
                SettingSection(
                    id = "general",
                    title = UiText.DynamicString("General"),
                    icon = Icons.Default.Info,
                    items = listOf(
                        SettingItem(
                            title = UiText.DynamicString("Theme"),
                            icon = Icons.Default.Info,
                            route = "theme_route"
                        )
                    )
                )
            ),
            expandedSectionIds = setOf("general")
        )

        composeTestRule.setContent {
            CestouTheme {
                ProfileScreen(
                    state = ProfileUiState(),
                    intent = ProfileIntent(),
                    settingsState = settingsState,
                    settingsIntent = SettingsIntent(onNavigate = { navigatedTo = it })
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_item_general_0").performScrollTo().performClick()

        assertEquals("theme_route", navigatedTo)
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
    fun `a Pro user sees the active subscription badge and can tap it to manage the plan`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var tapped = false

        composeTestRule.setContent {
            CestouTheme {
                ProfileScreen(
                    state = ProfileUiState(),
                    intent = ProfileIntent(onManageSubscription = { tapped = true }),
                    subscriptionStatus = SubscriptionStatus.PRO
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_subscription_pro_subtitle)).performClick()
        assert(tapped)
    }
}
