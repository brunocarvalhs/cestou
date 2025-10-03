package br.com.brunocarvalhs.howmuch.app.modules.base

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class BaseScreenTest : ComposeTest() {
    @Test
    fun baseScreen_rendersWithoutCrash() {
        composeTestRule.setContent {
            BaseScreen(tabs = linkedMapOf())
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
