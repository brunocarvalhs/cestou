package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.DataSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.DataSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Runs on the CI emulator. Text comes from resources so the test passes in any device locale.
@RunWith(AndroidJUnit4::class)
class DataSettingsScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var deleteAccountCalls = 0
    private var deleteAllCalls = 0

    private fun text(@StringRes id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun launch() = composeTestRule.setContent {
        CestouTheme {
            DataSettingsScreen(
                state = DataSettingsUiState(),
                intent = DataSettingsIntent(
                    onDeleteAccount = { deleteAccountCalls++ },
                    onDeleteAllData = { deleteAllCalls++ }
                )
            )
        }
    }

    @Test
    fun confirmingDeleteAccountDeletesTheAccount() {
        launch()

        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_account)).performClick()
        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_account_confirmation_title)).assertExists()
        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_account_button)).performClick()

        composeTestRule.waitForIdle()
        assertEquals(1, deleteAccountCalls)
        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_account_confirmation_title))
            .assertDoesNotExist()
    }

    @Test
    fun cancellingDeleteAccountKeepsTheAccount() {
        launch()

        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_account)).performClick()
        composeTestRule.onNodeWithText(text(R.string.action_cancel)).performClick()

        composeTestRule.waitForIdle()
        assertEquals(0, deleteAccountCalls)
        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_account_confirmation_title))
            .assertDoesNotExist()
    }

    @Test
    fun confirmingDeleteAllDataDeletesOnlyTheData() {
        launch()

        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_all)).performClick()
        composeTestRule.onNodeWithText(text(R.string.settings_data_delete_button)).performClick()

        composeTestRule.waitForIdle()
        assertEquals(1, deleteAllCalls)
        assertEquals(0, deleteAccountCalls)
    }
}
