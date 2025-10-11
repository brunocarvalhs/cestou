package br.com.brunocarvalhs.howmuch

import androidx.compose.ui.test.junit4.createComposeRule
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.TestAnalytics
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.mockkObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    manifest = Config.NONE,
    application = TestApplication::class
)
abstract class BaseTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    open fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(AnalyticsEvents)
        TestAnalytics.setupMocks()
    }

    @After
    open fun tearDown() {
        clearAllMocks()
    }
}
