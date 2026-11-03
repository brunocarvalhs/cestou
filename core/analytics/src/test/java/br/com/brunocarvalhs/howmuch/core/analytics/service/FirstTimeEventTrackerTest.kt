package br.com.brunocarvalhs.howmuch.core.analytics.service

import android.content.Context
import android.content.SharedPreferences
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FirstTimeEventTrackerTest {

    private val seen = mutableSetOf<String>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
        every { putBoolean(any(), any()) } answers {
            seen += firstArg<String>()
            this@mockk
        }
    }
    private val prefs = mockk<SharedPreferences> {
        every { getBoolean(any(), any()) } answers { firstArg<String>() in seen }
        every { edit() } returns editor
    }
    private val context = mockk<Context> {
        every { getSharedPreferences(any(), any()) } returns prefs
    }
    private val analytics = mockk<AnalyticsTracker>(relaxed = true) {
        every { isCollectionEnabled } returns true
    }
    private val tracker = FirstTimeEventTracker(context, analytics)

    @Test
    fun `trackOnce sends the event only the first time`() {
        tracker.trackOnce(AnalyticsEvents.FIRST_LIST_CREATED)
        tracker.trackOnce(AnalyticsEvents.FIRST_LIST_CREATED)

        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvents.FIRST_LIST_CREATED, any()) }
    }

    @Test
    fun `trackFeatureFirstUsed is independent per feature`() {
        tracker.trackFeatureFirstUsed("ai_chat")
        tracker.trackFeatureFirstUsed("ai_chat")
        tracker.trackFeatureFirstUsed("photo_scan")

        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvents.FEATURE_FIRST_USED, mapOf("feature" to "ai_chat")) }
        verify(
            exactly = 1
        ) { analytics.trackEvent(AnalyticsEvents.FEATURE_FIRST_USED, mapOf("feature" to "photo_scan")) }
    }
}
