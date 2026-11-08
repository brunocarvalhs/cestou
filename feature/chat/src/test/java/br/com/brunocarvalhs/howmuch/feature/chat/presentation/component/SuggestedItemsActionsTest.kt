package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SuggestedItemsActionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val added = mutableListOf<List<String>>()

    private fun actions(items: List<String>) = composeTestRule.setContent {
        CestouTheme {
            SuggestedItemsActions(
                items = items,
                onAddItem = { added += listOf(it) },
                onAddAll = { added += it },
            )
        }
    }

    @Test
    fun `plus on an item adds only that item`() {
        actions(listOf("Picanha", "Carvão"))

        composeTestRule.onNodeWithTag("ai_suggested_item_add_1").performClick()

        assertEquals(listOf(listOf("Carvão")), added)
    }

    @Test
    fun `add all sends every item`() {
        actions(listOf("Picanha", "Carvão"))

        composeTestRule.onNodeWithTag("ai_suggested_items_add_all").performClick()

        assertEquals(listOf(listOf("Picanha", "Carvão")), added)
    }

    @Test
    fun `a single item has no add all button`() {
        actions(listOf("Picanha"))

        composeTestRule.onNodeWithTag("ai_suggested_items_add_all").assertDoesNotExist()
    }
}
