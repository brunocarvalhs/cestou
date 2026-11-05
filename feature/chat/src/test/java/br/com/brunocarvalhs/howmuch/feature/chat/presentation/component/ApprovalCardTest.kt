package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalRequest
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** sdd-037: o card pede a decisão e nada executa sem o toque em Aprovar. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en")
class ApprovalCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        request: ApprovalRequest,
        onApprove: () -> Unit = {},
        onReject: () -> Unit = {}
    ) = composeTestRule.setContent {
        CestouTheme { ApprovalCard(request = request, onApprove = onApprove, onReject = onReject) }
    }

    @Test
    fun `shows the title, the list name and both buttons`() {
        show(ApprovalRequest("delete_shopping_list", "Mercado do mês"))

        composeTestRule.onNodeWithText("The AI wants to delete a list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_approval_summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mercado do mês").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_approval_approve").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_approval_reject").assertIsDisplayed()
    }

    @Test
    fun `without a summary only the generic lines show`() {
        show(ApprovalRequest("clear_purchased_products", summary = null))

        composeTestRule.onNodeWithText("The AI wants to remove the purchased items").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_approval_summary").assertDoesNotExist()
    }

    @Test
    fun `an unknown action falls back to the generic title`() {
        show(ApprovalRequest("some_future_action", summary = null))

        composeTestRule.onNodeWithText("The AI wants to make a change that cannot be undone").assertIsDisplayed()
    }

    @Test
    fun `tapping approve and reject call their own callback once`() {
        var approved = 0
        var rejected = 0
        show(ApprovalRequest("delete_shopping_list", null), onApprove = { approved++ }, onReject = { rejected++ })

        composeTestRule.onNodeWithTag("ai_approval_approve").performClick()
        composeTestRule.onNodeWithTag("ai_approval_reject").performClick()

        assertEquals(1, approved)
        assertEquals(1, rejected)
    }
}
