package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearSearchUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearSearchViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WearSearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun shopping(id: String, price: Double = 0.0) = Shopping(
        id = id,
        title = "Lista $id",
        description = "",
        price = price,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    private fun viewModel(state: WearSearchUiState) = mockk<WearSearchViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
    }

    @Test
    fun `search lists active and archived lists and opens the tapped one`() {
        var opened: String? = null
        val state = WearSearchUiState(
            active = StableList(listOf(shopping("a"))),
            archived = StableList(listOf(shopping("b", price = 12.5))),
            isLoading = false
        )
        composeTestRule.setContent {
            WearSearchScreen(viewModel = viewModel(state), onNavigateToDetail = { opened = it })
        }

        composeTestRule.onNodeWithTag("${TAG_SEARCH_ROW_PREFIX}a").assertIsDisplayed()
        composeTestRule.onNodeWithTag("${TAG_SEARCH_ROW_PREFIX}b").assertIsDisplayed()

        composeTestRule.onNodeWithTag("${TAG_SEARCH_ROW_PREFIX}a").performClick()

        assertEquals("a", opened)
    }

    @Test
    fun `search shows the empty state when nothing matches the query`() {
        val state = WearSearchUiState(query = "xyz", isLoading = false)
        composeTestRule.setContent {
            WearSearchScreen(viewModel = viewModel(state), onNavigateToDetail = {})
        }

        composeTestRule.onNodeWithTag(TAG_SEARCH_EMPTY).assertIsDisplayed()
    }

    @Test
    fun `search hides the empty state while lists are still loading`() {
        val state = WearSearchUiState(isLoading = true)
        composeTestRule.setContent {
            WearSearchScreen(viewModel = viewModel(state), onNavigateToDetail = {})
        }

        composeTestRule.onAllNodesWithTag(TAG_SEARCH_EMPTY).assertCountEquals(0)
    }
}
