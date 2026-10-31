package br.com.brunocarvalhs.howmuch.core.billing

import android.app.Activity
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingLifecycleObserverTest {

    @Test
    fun `onActivityResumed triggers callback on first call`() {
        var callCount = 0
        val observer = BillingLifecycleObserver(throttleMs = 3_000L) {
            callCount++
        }

        observer.onActivityResumed(mockk<Activity>())

        assertEquals(1, callCount)
    }

    @Test
    fun `onActivityResumed throttles rapid successive calls`() {
        var callCount = 0
        val observer = BillingLifecycleObserver(throttleMs = 3_000L) {
            callCount++
        }

        val activity = mockk<Activity>()
        observer.onActivityResumed(activity)
        observer.onActivityResumed(activity)
        observer.onActivityResumed(activity)

        assertEquals(1, callCount)
    }

    @Test
    fun `onActivityResumed triggers again after throttle period has passed`() {
        var callCount = 0
        val observer = BillingLifecycleObserver(throttleMs = 0L) {
            callCount++
        }

        val activity = mockk<Activity>()
        observer.onActivityResumed(activity)
        observer.onActivityResumed(activity)

        assertEquals(2, callCount)
    }
}
