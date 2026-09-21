package br.com.brunocarvalhs.howmuch.core.analytics.service

import android.os.Bundle
import br.com.brunocarvalhs.howmuch.core.analytics.exception.AnalyticsException
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirebaseAnalyticsTrackerTest {

    private val analytics = mockk<FirebaseAnalytics>(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val tracker = FirebaseAnalyticsTracker(analytics, crashReporter)

    @Before
    fun setup() {
        mockkConstructor(Bundle::class)
        every { anyConstructed<Bundle>().putString(any(), any()) } returns Unit
        every { anyConstructed<Bundle>().putInt(any(), any()) } returns Unit
        every { anyConstructed<Bundle>().putLong(any(), any()) } returns Unit
        every { anyConstructed<Bundle>().putDouble(any(), any()) } returns Unit
        every { anyConstructed<Bundle>().putFloat(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkConstructor(Bundle::class)
    }

    @Test
    fun `trackEvent forwards event name and bundled params to FirebaseAnalytics`() {
        val bundleSlot = slot<Bundle>()
        every { analytics.logEvent(any(), capture(bundleSlot)) } returns Unit

        tracker.trackEvent("shopping_list_created", mapOf("shopping_id" to "list-1"))

        verify { analytics.logEvent("shopping_list_created", bundleSlot.captured) }
        verify { bundleSlot.captured.putString("shopping_id", "list-1") }
    }

    @Test
    fun `trackEvent skips null params`() {
        tracker.trackEvent("event", mapOf("nullable" to null))

        verify(exactly = 0) { anyConstructed<Bundle>().putString("nullable", any()) }
    }

    @Test
    fun `trackEvent swallows and reports exceptions instead of crashing the caller`() {
        every { analytics.logEvent(any(), any()) } throws IllegalStateException("boom")

        tracker.trackEvent("event")

        verify { crashReporter.recordException(any<AnalyticsException>()) }
    }

    @Test
    fun `trackScreenView logs a screen_view event with screen name and class`() {
        tracker.trackScreenView(screenName = "shopping_list", screenClass = "ShoppingListViewModel")

        verify { analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
    }

    @Test
    fun `setUserId forwards to FirebaseAnalytics`() {
        tracker.setUserId("user-1")

        verify { analytics.setUserId("user-1") }
    }

    @Test
    fun `setUserProperty forwards to FirebaseAnalytics`() {
        tracker.setUserProperty("plan", "premium")

        verify { analytics.setUserProperty("plan", "premium") }
    }
}
