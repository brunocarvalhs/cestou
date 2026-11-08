package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.ChainQuote
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketChain
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketPriceComparison
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.PriceComparatorIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.PriceComparatorUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PriceComparatorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val location = UserLocation("RJ", "Niterói")
    private val cheap = SupermarketChain("cheap", "Cheap")
    private val pricey = SupermarketChain("pricey", "Pricey")
    private fun comparison(base: Double = 100.0, unpriced: Int = 0, empty: Boolean = false) =
        SupermarketPriceComparison(
            location = location,
            baseTotal = base,
            unpricedCount = unpriced,
            quotes = if (empty) {
                emptyList()
            } else {
                listOf(ChainQuote(cheap, 80.0, 30.0), ChainQuote(pricey, 110.0, 0.0))
            }
        )

    private fun show(state: PriceComparatorUiState, intent: PriceComparatorIntent = PriceComparatorIntent()) {
        composeTestRule.setContent { CestouTheme { PriceComparatorScreen(state = state, intent = intent) } }
    }

    private fun loaded(comparison: SupermarketPriceComparison) =
        PriceComparatorUiState(isLoading = false, location = location, comparison = comparison)

    @Test
    fun `shows a loading indicator while loading`() {
        show(PriceComparatorUiState(isLoading = true))

        composeTestRule.onNodeWithTag("price_comparator_loading").assertIsDisplayed()
    }

    @Test
    fun `always shows the estimate disclaimer`() {
        show(loaded(comparison()))

        composeTestRule.onNodeWithTag("price_comparator_disclaimer").assertIsDisplayed()
    }

    @Test
    fun `without a location it shows the form with save disabled until it is filled`() {
        show(PriceComparatorUiState(isLoading = false))

        composeTestRule.onNodeWithTag("price_comparator_location_form").assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_comparator_save_location").assertIsNotEnabled()
    }

    @Test
    fun `filling state and city saves them through the intent`() {
        var saved: Pair<String, String>? = null
        show(
            PriceComparatorUiState(isLoading = false),
            PriceComparatorIntent(onSaveLocation = { state, city -> saved = state to city })
        )

        composeTestRule.onNodeWithTag("price_comparator_state_field").performTouchInput { click() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("price_comparator_state_RJ").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag("price_comparator_state_field").assertTextContains("RJ")
        composeTestRule.onNodeWithTag("price_comparator_city_field").performTextInput("Niterói")
        composeTestRule.onNodeWithTag("price_comparator_city_field").assertTextContains("Niterói")
        composeTestRule.onNodeWithTag("price_comparator_save_location").assertIsEnabled().performClick()

        assertEquals("RJ" to "Niterói", saved)
    }

    @Test
    fun `ranks the chains and highlights the cheapest`() {
        show(loaded(comparison()))

        composeTestRule.onNodeWithTag("price_comparator_headline").assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_comparator_quote_cheap").assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_comparator_quote_pricey").assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_comparator_cheapest_badge").assertIsDisplayed()
    }

    @Test
    fun `warns about items left out for lack of price`() {
        show(loaded(comparison(unpriced = 2)))

        composeTestRule.onNodeWithTag("price_comparator_unpriced").assertIsDisplayed()
    }

    @Test
    fun `asks for prices when nothing is priced`() {
        show(loaded(comparison(base = 0.0)))

        composeTestRule.onNodeWithTag("price_comparator_no_prices").assertIsDisplayed()
    }

    @Test
    fun `says so when the state has no chains`() {
        show(loaded(comparison(empty = true)))

        composeTestRule.onNodeWithTag("price_comparator_no_chains").assertIsDisplayed()
    }

    @Test
    fun `change location goes through the intent`() {
        var changed = false
        show(loaded(comparison()), PriceComparatorIntent(onChangeLocation = { changed = true }))

        composeTestRule.onNodeWithTag("price_comparator_change_location").performClick()

        assertEquals(true, changed)
    }

    @Test
    fun `shows the error`() {
        show(PriceComparatorUiState(isLoading = false, error = UiText.StringResource(R.string.price_comparator_error)))

        composeTestRule.onNodeWithTag("price_comparator_error").assertIsDisplayed()
    }
}
