package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** sdd-036: a legenda traz o texto da faixa; a cor sozinha não basta. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en")
class CestouBudgetProgressTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        progress: BudgetProgress,
        caption: @Composable (BudgetProgress) -> String
    ) {
        composeTestRule.setContent {
            CestouTheme {
                CestouBudgetProgress(progress = progress, caption = caption(progress))
            }
        }
    }

    @Test
    fun `a list under budget shows the percent and what is left`() {
        show(BudgetProgress(limit = 200.0, spent = 50.0)) { budgetListCaption(it) }

        composeTestRule.onNodeWithText("25%", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("left", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a list over budget says by how much it went over`() {
        show(BudgetProgress(limit = 100.0, spent = 130.0)) { budgetListCaption(it) }

        composeTestRule.onNodeWithText("Over budget by", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the month caption shows the percent, the spent and the goal`() {
        show(BudgetProgress(limit = 1000.0, spent = 450.0)) { budgetMonthCaption(it) }

        composeTestRule.onNodeWithText("Monthly goal: 45%", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the month over the goal says it was exceeded`() {
        show(BudgetProgress(limit = 1000.0, spent = 1200.0)) { budgetMonthCaption(it) }

        composeTestRule.onNodeWithText("Monthly goal exceeded by", substring = true).assertIsDisplayed()
    }

    @Test
    fun `without a valid limit nothing is drawn`() {
        show(BudgetProgress(limit = null, spent = 50.0)) { "should not appear" }

        composeTestRule.onNodeWithText("should not appear").assertDoesNotExist()
    }
}
