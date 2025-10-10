package br.com.brunocarvalhs.howmuch.app.modules.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class MenuScreenTest : ComposeTest() {
    @Test
    fun menuScreen_rendersWithoutCrash() {
        composeTestRule.setContent {
            MenuScreen()
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
