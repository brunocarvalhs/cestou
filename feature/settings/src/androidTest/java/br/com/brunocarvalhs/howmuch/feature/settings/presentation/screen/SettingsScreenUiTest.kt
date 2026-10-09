package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.SupportContact
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingItem
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingSection
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingsUiState
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.SUPPORT_EMAIL
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingTheSupportContactRowNavigatesToContact() {
        var navigatedTo: Any? = null
        val state = SettingsUiState(
            sections = listOf(
                SettingSection(
                    title = UiText.DynamicString("Support"),
                    items = listOf(
                        SettingItem(
                            title = UiText.DynamicString("Contact"),
                            subtitle = UiText.DynamicString(SUPPORT_EMAIL),
                            icon = Icons.Outlined.Email,
                            route = SupportContact
                        )
                    )
                )
            )
        )

        composeTestRule.setContent {
            CestouTheme {
                SettingsScreen(state = state, intent = SettingsIntent(onNavigate = { navigatedTo = it }))
            }
        }

        composeTestRule.onNodeWithText(SUPPORT_EMAIL).assertExists()
        composeTestRule.onNodeWithText("Contact").performClick()

        composeTestRule.waitForIdle()
        assertEquals(SupportContact, navigatedTo)
    }
}
