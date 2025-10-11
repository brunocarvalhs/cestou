package br.com.brunocarvalhs.howmuch.app.modules.history.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class HistoryFilterTest : ComposeTest() {
    @Test
    fun historyFilter_rendersWithoutCrash() {
        composeTestRule.setContent {
            HistoryFilter(
                selectedFilter = HistoryFilterType.ALL,
                onFilterSelected = {
                    // Handle filter selection
                }
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
