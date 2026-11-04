package br.com.brunocarvalhs.howmuch.feature.feedback.data.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FeedbackStateStoreImplTest {

    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("feedback_state_test", Context.MODE_PRIVATE)
    private var now = 1_000L

    private fun store() = FeedbackStateStoreImpl(prefs, ::now)

    private val nps = FeedbackPrompt(SurveyKind.NPS)
    private val csat = FeedbackPrompt(SurveyKind.CSAT, "purchase")

    @Test
    fun `sessions and the first seen stamp persist across instances`() {
        store().recordSession()
        now = 5_000L
        store().recordSession()

        val state = store().load()

        assertEquals(2, state.sessions)
        assertEquals(1_000L, state.firstSeenAt)
    }

    @Test
    fun `answers store the cooldown per kind and feature, and clear dismissals`() {
        val store = store()
        store.recordDismissed(nps)
        store.recordAnswered(csat, score = 5)
        now = 2_000L
        store.recordAnswered(nps, score = 9)

        val state = store().load()

        assertEquals(mapOf("purchase" to 1_000L), state.lastCsatAtByFeature)
        assertEquals(2_000L, state.lastNpsAt)
        assertEquals(0, state.consecutiveDismissals)
        assertEquals(0L, state.lastLowScoreAt)
    }

    @Test
    fun `low scores are remembered`() {
        store().recordAnswered(nps, score = 6)
        assertEquals(1_000L, store().load().lastLowScoreAt)

        prefs.edit().clear().commit()
        store().recordAnswered(csat, score = 2)
        assertEquals(1_000L, store().load().lastLowScoreAt)

        prefs.edit().clear().commit()
        store().recordAnswered(csat, score = 3)
        assertEquals(0L, store().load().lastLowScoreAt)
    }

    @Test
    fun `dismissals accumulate and shown, purchases and store review are stamped`() {
        val store = store()
        store.recordDismissed(csat)
        store.recordDismissed(csat)
        store.recordShown(csat)
        store.recordPurchaseFinished()
        store.recordStoreReviewRequested()

        val state = store().load()

        assertEquals(2, state.consecutiveDismissals)
        assertEquals(1_000L, state.lastShownAt)
        assertEquals(1, state.purchasesFinished)
        assertEquals(1_000L, state.lastStoreReviewAt)
    }

    @Test
    fun `showing the store review is not a survey`() {
        store().recordShown(FeedbackPrompt(SurveyKind.STORE_REVIEW))

        assertEquals(0L, store().load().lastShownAt)
    }
}
