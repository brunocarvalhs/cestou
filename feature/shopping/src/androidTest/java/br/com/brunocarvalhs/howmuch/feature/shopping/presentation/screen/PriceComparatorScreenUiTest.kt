package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.ChainQuote
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketChain
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketPriceComparison
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.PriceComparatorIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.PriceComparatorUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PriceComparatorScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val location = UserLocation("RJ", "Niterói")
    private val state = PriceComparatorUiState(
        isLoading = false,
        location = location,
        comparison = SupermarketPriceComparison(
            location = location,
            baseTotal = 100.0,
            unpricedCount = 0,
            quotes = listOf(
                ChainQuote(SupermarketChain("cheap", "Cheap"), 80.0, 30.0),
                ChainQuote(SupermarketChain("pricey", "Pricey"), 110.0, 0.0)
            )
        )
    )

    @Test
    fun rankingShowsEveryChainAndTheCheapestBadge() {
        composeTestRule.setContent {
            CestouTheme { PriceComparatorScreen(state = state, intent = PriceComparatorIntent()) }
        }

        composeTestRule.onNodeWithTag("price_comparator_quote_cheap").assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_comparator_quote_pricey").assertIsDisplayed()
        composeTestRule.onNodeWithTag("price_comparator_cheapest_badge").assertIsDisplayed()
    }

    @Test
    fun changeLocationGoesThroughTheIntent() {
        var changed = false
        composeTestRule.setContent {
            CestouTheme {
                PriceComparatorScreen(state = state, intent = PriceComparatorIntent(onChangeLocation = { changed = true }))
            }
        }

        composeTestRule.onNodeWithTag("price_comparator_change_location").performClick()

        assertTrue(changed)
    }
}
