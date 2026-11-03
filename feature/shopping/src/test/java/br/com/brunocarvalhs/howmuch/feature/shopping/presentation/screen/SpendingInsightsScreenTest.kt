package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.toSpending
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.historyMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.SpendingInsightsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.CategoryShare
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.SpendingInsightsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SpendingInsightsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sep = historyMonth(2026, 9, "s1")
    private val nov = historyMonth(2026, 11, "n1", "n2")
    private val loaded = SpendingInsightsUiState(
        isLoading = false,
        months = StableList(listOf(sep.toSpending(), nov.toSpending())),
        selectedMonth = YearMonth.of(2026, 11),
        purchases = StableList(nov.purchases),
        categories = StableList(
            listOf(
                CategoryShare(ProductCategory.CARNES, 120.0, 0.6f),
                CategoryShare(null, 80.0, 0.4f)
            )
        )
    )

    private fun show(state: SpendingInsightsUiState, intent: SpendingInsightsIntent = SpendingInsightsIntent()) {
        composeTestRule.setContent { CestouTheme { SpendingInsightsScreen(state = state, intent = intent) } }
    }

    /** Lazy items off screen are not composed, so the list is scrolled to the node first. */
    private fun scrollTo(tag: String) {
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun `shows the loading indicator`() {
        show(SpendingInsightsUiState(isLoading = true))

        composeTestRule.onNodeWithTag("insights_loading").assertIsDisplayed()
    }

    @Test
    fun `shows the empty state`() {
        show(SpendingInsightsUiState(isLoading = false))

        composeTestRule.onNodeWithTag("insights_empty").assertIsDisplayed()
    }

    @Test
    fun `shows the error and retry reports the intent`() {
        var retried = false
        show(
            SpendingInsightsUiState(isLoading = false, error = UiText.DynamicString("boom")),
            SpendingInsightsIntent(onRetry = { retried = true })
        )

        composeTestRule.onNodeWithTag("insights_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("insights_retry").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun `tapping a bar selects that month`() {
        var selected: YearMonth? = null
        show(loaded, SpendingInsightsIntent(onMonthSelected = { selected = it }))

        composeTestRule.onNodeWithTag("insights_month_2026-09").performClick()

        assertEquals(YearMonth.of(2026, 9), selected)
    }

    @Test
    fun `ranking shows the categories and the others bucket`() {
        show(loaded)

        scrollTo("insights_category_1")
        composeTestRule.onNodeWithTag("insights_category_0").assertExists()
        composeTestRule.onNodeWithTag("insights_category_1").assertExists()
    }

    @Test
    fun `a failure in the ranking leaves the chart in place`() {
        show(loaded.copy(categories = StableList(), categoriesError = UiText.DynamicString("nope")))

        composeTestRule.onNodeWithTag("insights_month_2026-11").assertIsDisplayed()
        composeTestRule.onNodeWithTag("insights_categories_error").assertExists()
    }

    @Test
    fun `tapping a purchase reports it`() {
        var opened: String? = null
        show(loaded, SpendingInsightsIntent(onPurchaseClick = { opened = it.shopping.id }))

        scrollTo("insights_purchase_n1")
        composeTestRule.onNodeWithTag("insights_purchase_n1").performClick()

        assertEquals("n1", opened)
    }

    @Test
    fun `back reports the intent`() {
        var back = false
        show(loaded, SpendingInsightsIntent(onBack = { back = true }))

        composeTestRule.onNodeWithTag("insights_back").performClick()

        assertEquals(true, back)
    }
}
