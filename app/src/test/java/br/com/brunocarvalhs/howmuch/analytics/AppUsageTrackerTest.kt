package br.com.brunocarvalhs.howmuch.analytics

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.util.concurrent.TimeUnit

class AppUsageTrackerTest {

    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private var now = TimeUnit.DAYS.toMillis(100)
    private val store = mutableMapOf<String, Any>()
    private fun tracker() = AppUsageTracker(contextWithPrefs(store), analytics) { now }

    @Test
    fun `foreground then background reports the session length in seconds`() {
        val tracker = tracker()

        tracker.onForeground()
        now += TimeUnit.SECONDS.toMillis(42)
        tracker.onBackground()

        verify { analytics.trackEvent(AnalyticsEvents.APP_FOREGROUND) }
        verify { analytics.trackEvent(AnalyticsEvents.APP_BACKGROUND, mapOf("session_seconds" to 42L)) }
    }

    @Test
    fun `a second foreground without a background is ignored`() {
        val tracker = tracker()

        tracker.onForeground()
        tracker.onForeground()

        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvents.APP_FOREGROUND) }
    }

    @Test
    fun `background without a foreground reports nothing`() {
        tracker().onBackground()

        verify(exactly = 0) { analytics.trackEvent(AnalyticsEvents.APP_BACKGROUND, any()) }
    }

    @Test
    fun `the first ever open is not a return`() {
        tracker().onForeground()

        verify(exactly = 0) { analytics.trackEvent(AnalyticsEvents.RETURNED_AFTER_DAYS, any()) }
    }

    @Test
    fun `coming back after five days reports the 4-7 bucket`() {
        val first = tracker()
        first.onForeground()
        first.onBackground()
        now += TimeUnit.DAYS.toMillis(5)

        tracker().onForeground()

        verify { analytics.trackEvent(AnalyticsEvents.RETURNED_AFTER_DAYS, mapOf("days" to "4-7")) }
    }

    @Test
    fun `opening twice on the same day is not a return`() {
        val first = tracker()
        first.onForeground()
        first.onBackground()

        tracker().onForeground()

        verify(exactly = 0) { analytics.trackEvent(AnalyticsEvents.RETURNED_AFTER_DAYS, any()) }
    }
}
