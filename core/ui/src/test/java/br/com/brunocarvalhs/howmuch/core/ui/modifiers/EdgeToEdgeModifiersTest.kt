package br.com.brunocarvalhs.howmuch.core.ui.modifiers

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EdgeToEdgeModifiersTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var top = 0f
    private var height = 0
    private var rootHeight = 0

    private fun showWithInsets(padding: Modifier.() -> Modifier) {
        composeTestRule.runOnUiThread { WindowCompat.setDecorFitsSystemWindows(composeTestRule.activity.window, false) }
        composeTestRule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { rootHeight = it.size.height }
                    .padding()
                    .onGloballyPositioned {
                        top = it.positionInRoot().y
                        height = it.size.height
                    },
            )
        }
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, STATUS, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, NAV))
            .build()
        composeTestRule.runOnUiThread {
            ViewCompat.dispatchApplyWindowInsets(composeTestRule.activity.window.decorView, insets)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `topBarEdgePadding only reserves the status bar`() {
        showWithInsets { topBarEdgePadding() }

        assertEquals(STATUS.toFloat(), top, 0f)
        assertEquals(rootHeight - STATUS, height)
    }

    @Test
    fun `screenEdgePadding reserves the status and navigation bars`() {
        showWithInsets { screenEdgePadding() }

        assertEquals(STATUS.toFloat(), top, 0f)
        assertEquals(rootHeight - STATUS - NAV, height)
    }

    private companion object {
        const val STATUS = 40
        const val NAV = 30
    }
}
