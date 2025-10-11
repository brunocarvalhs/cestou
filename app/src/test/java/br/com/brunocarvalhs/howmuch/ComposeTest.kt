package br.com.brunocarvalhs.howmuch

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    manifest = Config.NONE,
    application = TestApplication::class
)
abstract class ComposeTest {
    init {
        ShadowLog.stream = System.out
    }

    @get:Rule
    val composeTestRule = createComposeRule()
}
