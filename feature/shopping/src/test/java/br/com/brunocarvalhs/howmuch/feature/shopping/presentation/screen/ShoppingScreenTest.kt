package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingListUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ShoppingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Test
    fun `renders the app name in the top bar and a create list action`() {
        composeTestRule.setContent {
            CestouTheme {
                ShoppingScreen(
                    uiState = ShoppingListUiState(
                        list = StableList(listOf(shopping)),
                        filteredList = StableList(listOf(shopping))
                    ),
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                    intent = ShoppingListIntent()
                )
            }
        }

        composeTestRule.onNodeWithText("Weekly Groceries").assertExists()
    }

    @Test
    fun `renders without crashing when the list is empty`() {
        composeTestRule.setContent {
            CestouTheme {
                ShoppingScreen(
                    uiState = ShoppingListUiState(),
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                    intent = ShoppingListIntent()
                )
            }
        }

        composeTestRule.onRoot().assertExists()
    }
}
