package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingItem
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da lista
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsSectionsListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sections = listOf(
        SettingSection(
            id = "general",
            title = UiText.DynamicString("General"),
            icon = Icons.Default.Info,
            items = listOf(
                SettingItem(title = UiText.DynamicString("Theme"), icon = Icons.Default.Info, route = "theme_route")
            )
        ),
        SettingSection(
            id = "about",
            title = UiText.DynamicString("About"),
            icon = Icons.Default.Info,
            items = listOf(
                SettingItem(title = UiText.DynamicString("Version"), icon = Icons.Default.Info, route = null)
            )
        )
    )

    @Test
    fun `a collapsed section does not expose its item testTags`() {
        composeTestRule.setContent {
            CestouTheme {
                SettingsSectionsList(
                    sections = sections,
                    expandedSectionIds = emptySet(),
                    intent = SettingsIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_item_general_0").assertDoesNotExist()
    }

    @Test
    fun `tapping a section header expands it and reveals its items`() {
        var expandedSectionId: String? = null

        composeTestRule.setContent {
            CestouTheme {
                SettingsSectionsList(
                    sections = sections,
                    expandedSectionIds = emptySet(),
                    intent = SettingsIntent(onToggleSection = { expandedSectionId = it })
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_section_header_general").performClick()

        assertEquals("general", expandedSectionId)
    }

    @Test
    fun `expanding a second section keeps the first one open - non-exclusive accordion`() {
        composeTestRule.setContent {
            CestouTheme {
                SettingsSectionsList(
                    sections = sections,
                    expandedSectionIds = setOf("general", "about"),
                    intent = SettingsIntent()
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_item_general_0").assertExists()
        composeTestRule.onNodeWithTag("settings_item_about_0").assertExists()
    }

    @Test
    fun `tapping a leaf item with a non-null route invokes onNavigate`() {
        var navigatedTo: Any? = null

        composeTestRule.setContent {
            CestouTheme {
                SettingsSectionsList(
                    sections = sections,
                    expandedSectionIds = setOf("general"),
                    intent = SettingsIntent(onNavigate = { navigatedTo = it })
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_item_general_0").performClick()

        assertEquals("theme_route", navigatedTo)
    }

    @Test
    fun `tapping a leaf item with a null route does not invoke onNavigate`() {
        var navigatedTo: Any? = null

        composeTestRule.setContent {
            CestouTheme {
                SettingsSectionsList(
                    sections = sections,
                    expandedSectionIds = setOf("about"),
                    intent = SettingsIntent(onNavigate = { navigatedTo = it })
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_item_about_0").performClick()

        assertNull(navigatedTo)
    }
}
