package br.com.brunocarvalhs.howmuch.push

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.PushPayload
import br.com.brunocarvalhs.howmuch.core.domain.model.PushSaveOutcome
import br.com.brunocarvalhs.howmuch.core.domain.usecase.SavePushNotificationUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PushNotificationHandlerTest {

    private val savePushNotification = mockk<SavePushNotificationUseCase>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val handler = PushNotificationHandler(savePushNotification, analyticsTracker)
    private val payload = PushPayload("m1", null, null, null, mapOf("title" to "SECRET", "message" to "SECRET"))

    private fun source(label: String) = mapOf(AnalyticsParams.SOURCE to label)

    @Test
    fun `a saved push is tracked as received and saved with its source`() = runTest {
        coEvery { savePushNotification(payload) } returns PushSaveOutcome.Saved

        val outcome = handler.handle(payload, PushSource.TAP)

        assertEquals(PushSaveOutcome.Saved, outcome)
        verify { analyticsTracker.trackEvent(AnalyticsEvents.PUSH_RECEIVED, source("tap")) }
        verify { analyticsTracker.trackEvent(AnalyticsEvents.PUSH_SAVED, source("tap")) }
    }

    @Test
    fun `a duplicate is received but neither saved nor failed`() = runTest {
        coEvery { savePushNotification(payload) } returns PushSaveOutcome.AlreadySaved

        handler.handle(payload, PushSource.MESSAGE)

        verify { analyticsTracker.trackEvent(AnalyticsEvents.PUSH_RECEIVED, source("message")) }
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.PUSH_SAVED, any()) }
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.PUSH_SAVE_FAILED, any()) }
    }

    @Test
    fun `an ignored push is tracked with its reason only`() = runTest {
        coEvery { savePushNotification(payload) } returns PushSaveOutcome.Ignored(PushSaveOutcome.IgnoreReason.NO_USER)

        handler.handle(payload, PushSource.MESSAGE)

        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.PUSH_IGNORED, mapOf(AnalyticsParams.REASON to "no_user"))
        }
    }

    @Test
    fun `a failure is tracked and returned`() = runTest {
        val error = IllegalStateException("offline")
        coEvery { savePushNotification(payload) } returns PushSaveOutcome.Failed(error)

        val outcome = handler.handle(payload, PushSource.MESSAGE)

        assertEquals(PushSaveOutcome.Failed(error), outcome)
        verify { analyticsTracker.trackEvent(AnalyticsEvents.PUSH_SAVE_FAILED, source("message")) }
    }

    @Test
    fun `the campaign text never reaches analytics`() = runTest {
        coEvery { savePushNotification(payload) } returns PushSaveOutcome.Saved

        handler.handle(payload, PushSource.MESSAGE)

        verify(exactly = 0) {
            analyticsTracker.trackEvent(
                any(),
                match { params -> params.values.any { it.toString().contains("SECRET") } }
            )
        }
    }
}
