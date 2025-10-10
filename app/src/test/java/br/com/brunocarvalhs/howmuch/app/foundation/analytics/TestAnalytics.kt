package br.com.brunocarvalhs.howmuch.app.foundation.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject

object TestAnalytics {
    private val analyticsInstance = mockk<FirebaseAnalytics>(relaxed = true)

    fun setupMocks() {
        mockkObject(AnalyticsEvents)
        every { AnalyticsEvents.analytics } returns analyticsInstance
    }
}
