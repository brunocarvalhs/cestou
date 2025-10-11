package br.com.brunocarvalhs.howmuch.app.modules.history.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import br.com.brunocarvalhs.howmuch.MainActivity
import br.com.brunocarvalhs.howmuch.app.foundation.constants.TIPS
import org.junit.Test
import org.robolectric.Robolectric

class TipsComponentTest : ComposeTest() {

    private val activity by lazy { Robolectric.buildActivity(MainActivity::class.java) }

    @Test
    fun tipsComponent_rendersWithoutCrash() {
        composeTestRule.setContent {
            TipsComponent(
                context = activity.get(),
                tips = TIPS,
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
