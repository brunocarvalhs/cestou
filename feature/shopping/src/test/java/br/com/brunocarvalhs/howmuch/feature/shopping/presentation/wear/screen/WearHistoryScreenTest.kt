package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearHistoryUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearHistoryViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WearHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun viewModel(state: WearHistoryUiState) = mockk<WearHistoryViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
    }

    @Test
    fun `history shows a row for each month with purchases`() {
        val month = HistoryMonth(
            key = YearMonth.of(2026, 11),
            total = 42.0,
            purchases = emptyList()
        )
        composeTestRule.setContent {
            WearHistoryScreen(
                viewModel = viewModel(WearHistoryUiState(months = StableList(listOf(month)), isLoading = false))
            )
        }

        composeTestRule.onNodeWithTag("${TAG_HISTORY_MONTH_PREFIX}2026-11").assertIsDisplayed()
    }

    @Test
    fun `history shows the empty state when there are no purchases`() {
        composeTestRule.setContent {
            WearHistoryScreen(viewModel = viewModel(WearHistoryUiState(isLoading = false)))
        }

        composeTestRule.onNodeWithTag(TAG_HISTORY_EMPTY).assertIsDisplayed()
    }
}
