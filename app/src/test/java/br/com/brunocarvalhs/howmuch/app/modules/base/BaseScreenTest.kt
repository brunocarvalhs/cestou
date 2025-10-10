package br.com.brunocarvalhs.howmuch.app.modules.base

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.NavBarItem
import org.junit.Test

class BaseScreenTest : ComposeTest() {
    @Test
    fun baseScreen_rendersWithoutCrash() {
        composeTestRule.setContent {
            BaseScreen(
                tabs = linkedMapOf(
                    NavBarItem.HOME to { },
                    NavBarItem.HISTORY to { },
                    NavBarItem.MENU to { }
                )
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
