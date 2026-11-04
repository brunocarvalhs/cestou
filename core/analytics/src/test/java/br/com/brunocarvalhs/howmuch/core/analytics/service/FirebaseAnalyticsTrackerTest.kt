package br.com.brunocarvalhs.howmuch.core.analytics.service

import android.os.Bundle
import br.com.brunocarvalhs.howmuch.core.analytics.exception.AnalyticsException
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseAnalyticsTrackerTest {

    private val analytics = mockk<FirebaseAnalytics>(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val featureFlags = mockk<FeatureFlagService> {
        every { isEnabled(any(), any()) } returns true
    }
    private val tracker = FirebaseAnalyticsTracker(analytics, crashReporter, featureFlags)

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

    @Test
    fun `trackEvent drops an invalid event name and reports it`() {
        tracker.trackEvent("1 invalid-name")

        verify(exactly = 0) { analytics.logEvent(any(), any()) }
        verify { crashReporter.recordException(any<AnalyticsException>()) }
    }

    @Test
    fun `trackEvent drops a reserved firebase prefix`() {
        tracker.trackEvent("firebase_custom")

        verify(exactly = 0) { analytics.logEvent(any(), any()) }
    }

    @Test
    fun `trackEvent truncates string values to 100 characters`() {
        tracker.trackEvent("event", mapOf("key" to "a".repeat(150)))

        verify { anyConstructed<Bundle>().putString("key", "a".repeat(100)) }
    }

    @Test
    fun `trackEvent keeps at most 25 params`() {
        val params = (1..30).associate { "p$it" to it }

        tracker.trackEvent("event", params)

        verify(exactly = 25) { anyConstructed<Bundle>().putInt(any(), any()) }
    }

    @Test
    fun `setCollectionEnabled forwards to FirebaseAnalytics`() {
        tracker.setCollectionEnabled(false)

        verify { analytics.setAnalyticsCollectionEnabled(false) }
    }

    @Test
    fun `setUserProperty truncates the value to 36 characters`() {
        tracker.setUserProperty("plan", "x".repeat(50))

        verify { analytics.setUserProperty("plan", "x".repeat(36)) }
    }

    @Test
    fun `the remote kill-switch drops ui_action but keeps every other event`() {
        every { featureFlags.isEnabled("feat_analytics_ui_actions", any()) } returns false

        tracker.trackEvent("ui_action", mapOf("element_id" to "cart.share"))
        tracker.trackEvent("product_added")

        verify(exactly = 0) { analytics.logEvent("ui_action", any()) }
        verify { analytics.logEvent("product_added", any()) }
    }

    @Test
    fun `isCollectionEnabled follows the last setCollectionEnabled`() {
        assertTrue(tracker.isCollectionEnabled)

        tracker.setCollectionEnabled(false)

        assertFalse(tracker.isCollectionEnabled)
    }
}
