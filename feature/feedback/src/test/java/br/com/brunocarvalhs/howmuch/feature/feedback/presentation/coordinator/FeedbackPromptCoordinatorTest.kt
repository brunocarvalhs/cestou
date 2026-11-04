package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.coordinator

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.feedback.data.config.FeedbackConfigSource
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackState
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class FeedbackPromptCoordinatorTest {

    private var now = TimeUnit.DAYS.toMillis(1000)
    private fun daysAgo(days: Long) = now - TimeUnit.DAYS.toMillis(days)

    private var state = FeedbackState(firstSeenAt = daysAgo(20), sessions = 5)
    private val store = mockk<FeedbackStateStore>(relaxed = true) {
        every { load() } answers { state }
    }
    private val analytics = mockk<AnalyticsTracker>(relaxed = true) {
        every { isCollectionEnabled } returns true
    }
    private val flags = mockk<FeatureFlagService> {
        every { isEnabled(any(), any()) } answers { secondArg() }
    }
    private val variables = mockk<RemoteVariableService> {
        every { getLong(any(), any()) } answers { secondArg() }
    }
    private val auth = mockk<AuthService> {
        every { currentUser } returns AuthenticatedUser(id = "u1", createdAt = daysAgo(30))
    }

    private val configSource = FeedbackConfigSource(flags, variables)

    private fun coordinator() = FeedbackPromptCoordinator(store, analytics, configSource, auth, ::currentTime)

    private fun currentTime() = now

    @Test
    fun `an eligible moment publishes a pending prompt`() {
        val c = coordinator()

        c.onMoment(FeedbackMoment.AI_CHAT_CLOSED)

        assertEquals(FeedbackPrompt(SurveyKind.CSAT, "ai_chat"), c.pending.value)
    }

    @Test
    fun `an ineligible moment does not publish`() {
        state = state.copy(lastShownAt = daysAgo(1))
        val c = coordinator()

        c.onMoment(FeedbackMoment.AI_CHAT_CLOSED)

        assertNull(c.pending.value)
    }

    @Test
    fun `a pending prompt is not replaced by a later moment`() {
        val c = coordinator()
        c.onMoment(FeedbackMoment.AI_CHAT_CLOSED)

        c.onMoment(FeedbackMoment.PHOTO_SCAN_CONFIRMED)

        assertEquals("ai_chat", c.pending.value?.feature)
    }

    @Test
    fun `consume clears the pending prompt, stamps it and reports the exhibition with its trigger`() {
        val c = coordinator()
        c.onMoment(FeedbackMoment.AI_CHAT_CLOSED)
        val prompt = c.pending.value!!

        c.consume(prompt)

        assertNull(c.pending.value)
        verify { store.recordShown(prompt) }
        verify {
            analytics.trackEvent(
                AnalyticsEvents.FEEDBACK_PROMPT_SHOWN,
                mapOf(
                    AnalyticsParams.KIND to "csat",
                    AnalyticsParams.FEATURE to "ai_chat",
                    AnalyticsParams.TRIGGER to "ai_chat_closed"
                )
            )
        }
    }

    @Test
    fun `only one survey per session`() {
        val c = coordinator()
        c.onMoment(FeedbackMoment.AI_CHAT_CLOSED)
        c.consume(c.pending.value!!)
        state = state.copy(lastShownAt = 0L)

        c.onMoment(FeedbackMoment.PHOTO_SCAN_CONFIRMED)

        assertNull(c.pending.value)
    }

    @Test
    fun `consuming a prompt that is not pending does nothing`() {
        val c = coordinator()

        c.consume(FeedbackPrompt(SurveyKind.NPS))

        verify(exactly = 0) { store.recordShown(any()) }
    }

    @Test
    fun `flag off and analytics off suppress everything`() {
        every { flags.isEnabled(FeatureFlagKeys.FEEDBACK_ENABLED, any()) } returns false
        val off = coordinator()
        off.onMoment(FeedbackMoment.AI_CHAT_CLOSED)
        assertNull(off.pending.value)
        assertFalse(off.isFeedbackEnabled())

        every { flags.isEnabled(any(), any()) } answers { secondArg() }
        every { analytics.isCollectionEnabled } returns false
        val optedOut = coordinator()
        optedOut.onMoment(FeedbackMoment.AI_CHAT_CLOSED)
        assertNull(optedOut.pending.value)
        assertFalse(optedOut.isFeedbackEnabled())
    }

    @Test
    fun `purchases are counted even while feedback is off`() {
        every { analytics.isCollectionEnabled } returns false
        val c = coordinator()

        c.onMoment(FeedbackMoment.PURCHASE_FINISHED)

        verify { store.recordPurchaseFinished() }
        assertNull(c.pending.value)
    }

    @Test
    fun `a finished purchase is counted and the third one asks for the purchase CSAT`() {
        every { store.recordPurchaseFinished() } answers {
            state = state.copy(purchasesFinished = state.purchasesFinished + 1)
        }
        val c = coordinator()
        repeat(3) { c.onMoment(FeedbackMoment.PURCHASE_FINISHED) }

        verify(exactly = 3) { store.recordPurchaseFinished() }
        assertEquals("purchase", c.pending.value?.feature)
    }

    @Test
    fun `a milestone without an eligible survey asks for the store review and records it`() {
        state = state.copy(purchasesFinished = 2, lastCsatAtByFeature = mapOf("purchase" to daysAgo(1)))
        every { store.recordPurchaseFinished() } answers { state = state.copy(purchasesFinished = 3) }
        val c = coordinator()

        c.onMoment(FeedbackMoment.PURCHASE_FINISHED)
        val prompt = c.pending.value!!
        c.consume(prompt)

        assertEquals(SurveyKind.STORE_REVIEW, prompt.kind)
        verify { store.recordStoreReviewRequested() }
        verify(exactly = 0) { store.recordShown(any()) }
        verify { analytics.trackEvent(AnalyticsEvents.STORE_REVIEW_REQUESTED, any()) }
    }

    @Test
    fun `sessions are counted once per 30 minute gap`() {
        val c = coordinator()
        c.onAppForeground()
        now += TimeUnit.MINUTES.toMillis(5)
        c.onAppForeground()
        now += TimeUnit.MINUTES.toMillis(40)
        c.onAppForeground()

        verify(exactly = 2) { store.recordSession() }
        assertTrue(c.pending.value == null)
    }
}
