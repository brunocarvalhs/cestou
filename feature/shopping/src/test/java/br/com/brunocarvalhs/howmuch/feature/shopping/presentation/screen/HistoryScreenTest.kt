package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.historyMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.HistoryIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.HistoryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val months = StableList(listOf(historyMonth(2026, 9, "s1"), historyMonth(2026, 11, "n1", "n2")))

    private fun show(state: HistoryUiState, intent: HistoryIntent = HistoryIntent()) {
        composeTestRule.setContent { CestouTheme { HistoryScreen(state = state, intent = intent) } }
    }

    @Test
    fun `shows the loading indicator`() {
        show(HistoryUiState(isLoading = true))

        composeTestRule.onNodeWithTag("history_loading").assertIsDisplayed()
    }

    @Test
    fun `shows the empty state when there are no purchases`() {
        show(HistoryUiState(isLoading = false))

        composeTestRule.onNodeWithTag("history_empty").assertIsDisplayed()
    }

    @Test
    fun `shows the error and retry reports the intent`() {
        var retried = false
        show(
            HistoryUiState(isLoading = false, error = UiText.DynamicString("boom")),
            HistoryIntent(onRetry = { retried = true })
        )

        composeTestRule.onNodeWithTag("history_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_retry").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun `lists the purchases of the selected month only`() {
        show(HistoryUiState(isLoading = false, months = months, selectedMonth = YearMonth.of(2026, 11)))

        composeTestRule.onNodeWithTag("history_purchase_n1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_purchase_n2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_purchase_s1").assertDoesNotExist()
    }

    @Test
    fun `tapping a month card reports that month`() {
        var selected: YearMonth? = null
        show(
            HistoryUiState(isLoading = false, months = months, selectedMonth = YearMonth.of(2026, 11)),
            HistoryIntent(onMonthSelected = { selected = it })
        )

        composeTestRule.onNodeWithTag("history_month_2026-09").performClick()

        assertEquals(YearMonth.of(2026, 9), selected)
    }

    @Test
    fun `tapping a purchase reports it`() {
        var opened: String? = null
        show(
            HistoryUiState(isLoading = false, months = months, selectedMonth = YearMonth.of(2026, 11)),
            HistoryIntent(onPurchaseClick = { opened = it.shopping.id })
        )

        composeTestRule.onNodeWithTag("history_purchase_n2").performClick()

        assertEquals("n2", opened)
    }

    @Test
    fun `the analysis shortcut reports the intent`() {
        var clicked = false
        show(
            HistoryUiState(isLoading = false, months = months, selectedMonth = YearMonth.of(2026, 11)),
            HistoryIntent(onSeeAnalysis = { clicked = true })
        )

        composeTestRule.onNodeWithTag("history_see_analysis").performClick()

        assertEquals(true, clicked)
    }
}
