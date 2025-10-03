package br.com.brunocarvalhs.howmuch.app.modules.history.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.ComposeTest
import br.com.brunocarvalhs.howmuch.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryTopBarTest : ComposeTest() {
    @Test
    fun historyTopBar_rendersWithoutCrash() {
        composeTestRule.setContent {
            HistoryTopBar(
                title = R.string.history,
                selectionMode = true,
                selectedCount = 2,
                totalItems = 5,
                onCancelSelection = {},
                onEnterSelectionMode = {},
                onDeleteSelected = {},
                onSelectAll = {},
                onNotSelectAll = {}
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
