package br.com.brunocarvalhs.howmuch.feature.feedback.domain.policy

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackConfig
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackState
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.PromptContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class FeedbackPolicyTest {

    private val now = TimeUnit.DAYS.toMillis(1000)
    private fun daysAgo(days: Long) = now - TimeUnit.DAYS.toMillis(days)

    private fun policy(config: FeedbackConfig = FeedbackConfig()) = FeedbackPolicy(config) { now }

    private val ctx = PromptContext(analyticsEnabled = true, accountAgeDays = 30, shownThisSession = false)

    private val veteran = FeedbackState(
        firstSeenAt = daysAgo(20),
        sessions = 5,
        purchasesFinished = 1
    )

    @Test
    fun `purchase milestone asks the purchase CSAT`() {
        val prompt = policy().evaluate(FeedbackMoment.PURCHASE_FINISHED, veteran, ctx)

        assertEquals(FeedbackPrompt(SurveyKind.CSAT, "purchase"), prompt)
    }

    @Test
    fun `second purchase does not ask but the third and later do`() {
        assertNull(policy().evaluate(FeedbackMoment.PURCHASE_FINISHED, veteran.copy(purchasesFinished = 2), ctx))
        assertEquals(
            "purchase",
            policy().evaluate(FeedbackMoment.PURCHASE_FINISHED, veteran.copy(purchasesFinished = 3), ctx)?.feature
        )
        assertEquals(
            "purchase",
            policy().evaluate(FeedbackMoment.PURCHASE_FINISHED, veteran.copy(purchasesFinished = 9), ctx)?.feature
        )
    }

    @Test
    fun `ai chat and photo scan moments map to their feature`() {
        assertEquals("ai_chat", policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran, ctx)?.feature)
        assertEquals("photo_scan", policy().evaluate(FeedbackMoment.PHOTO_SCAN_CONFIRMED, veteran, ctx)?.feature)
    }

    @Test
    fun `never asks with analytics off`() {
        assertNull(policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran, ctx.copy(analyticsEnabled = false)))
    }

    @Test
    fun `never asks with the remote flag off`() {
        assertNull(policy(FeedbackConfig(enabled = false)).evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran, ctx))
    }

    @Test
    fun `never asks a brand new account`() {
        assertNull(policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran, ctx.copy(accountAgeDays = 0)))
    }

    @Test
    fun `an unknown account age does not block`() {
        assertEquals(
            "ai_chat",
            policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran, ctx.copy(accountAgeDays = null))?.feature
        )
    }

    @Test
    fun `asks at most once per session`() {
        assertNull(policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran, ctx.copy(shownThisSession = true)))
    }

    @Test
    fun `any survey in the last 7 days blocks every other`() {
        assertNull(policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran.copy(lastShownAt = daysAgo(6)), ctx))
        assertEquals(
            "ai_chat",
            policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran.copy(lastShownAt = daysAgo(7)), ctx)?.feature
        )
    }

    @Test
    fun `csat has its own cooldown per feature`() {
        val state = veteran.copy(lastCsatAtByFeature = mapOf("ai_chat" to daysAgo(10)))

        assertNull(policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, state, ctx))
        assertEquals("photo_scan", policy().evaluate(FeedbackMoment.PHOTO_SCAN_CONFIRMED, state, ctx)?.feature)
        assertEquals(
            "ai_chat",
            policy(FeedbackConfig(csatCooldownDays = 5)).evaluate(FeedbackMoment.AI_CHAT_CLOSED, state, ctx)?.feature
        )
    }

    @Test
    fun `nps needs 7 days since first use and 3 sessions`() {
        val moment = FeedbackMoment.APP_RELATIONSHIP

        assertEquals(FeedbackPrompt(SurveyKind.NPS), policy().evaluate(moment, veteran, ctx))
        assertNull(policy().evaluate(moment, veteran.copy(firstSeenAt = daysAgo(6)), ctx))
        assertNull(policy().evaluate(moment, veteran.copy(sessions = 2), ctx))
        assertNull(policy().evaluate(moment, veteran.copy(firstSeenAt = 0L), ctx))
    }

    @Test
    fun `nps respects its 90 day cooldown`() {
        val moment = FeedbackMoment.APP_RELATIONSHIP

        assertNull(policy().evaluate(moment, veteran.copy(lastNpsAt = daysAgo(89)), ctx))
        assertEquals(
            SurveyKind.NPS,
            policy().evaluate(moment, veteran.copy(lastNpsAt = daysAgo(91), lastShownAt = daysAgo(91)), ctx)?.kind
        )
    }

    @Test
    fun `nps is only asked on the relationship moment`() {
        assertEquals(SurveyKind.CSAT, policy().evaluate(FeedbackMoment.AI_CHAT_CLOSED, veteran, ctx)?.kind)
    }

    @Test
    fun `dismissals back off 14 then 45 then 90 days`() {
        fun asks(count: Int, daysSinceDismissal: Long) = policy().evaluate(
            FeedbackMoment.AI_CHAT_CLOSED,
            veteran.copy(consecutiveDismissals = count, lastDismissedAt = daysAgo(daysSinceDismissal)),
            ctx
        ) != null

        assertFalse(asks(1, 13))
        assertTrue(asks(1, 14))
        assertFalse(asks(2, 44))
        assertTrue(asks(2, 45))
        assertFalse(asks(3, 89))
        assertTrue(asks(5, 90))
    }

    private val reviewReady = veteran.copy(
        firstSeenAt = daysAgo(20),
        purchasesFinished = 3,
        lastShownAt = daysAgo(10)
    )

    @Test
    fun `store review is requested by a usage milestone`() {
        assertTrue(policy().shouldRequestStoreReview(reviewReady, ctx))
    }

    @Test
    fun `store review waits for the third purchase and 14 days`() {
        assertFalse(policy().shouldRequestStoreReview(reviewReady.copy(purchasesFinished = 2), ctx))
        assertFalse(policy().shouldRequestStoreReview(reviewReady.copy(firstSeenAt = daysAgo(13)), ctx))
    }

    @Test
    fun `store review is never right after a survey nor after a low score`() {
        assertFalse(policy().shouldRequestStoreReview(reviewReady.copy(lastShownAt = daysAgo(2)), ctx))
        assertFalse(policy().shouldRequestStoreReview(reviewReady.copy(lastLowScoreAt = daysAgo(29)), ctx))
        assertTrue(policy().shouldRequestStoreReview(reviewReady.copy(lastLowScoreAt = daysAgo(31)), ctx))
    }

    @Test
    fun `store review is requested at most every 120 days and honors both flags`() {
        assertFalse(policy().shouldRequestStoreReview(reviewReady.copy(lastStoreReviewAt = daysAgo(119)), ctx))
        assertTrue(policy().shouldRequestStoreReview(reviewReady.copy(lastStoreReviewAt = daysAgo(121)), ctx))
        assertFalse(policy(FeedbackConfig(storeReviewEnabled = false)).shouldRequestStoreReview(reviewReady, ctx))
        assertFalse(policy(FeedbackConfig(enabled = false)).shouldRequestStoreReview(reviewReady, ctx))
        assertFalse(policy().shouldRequestStoreReview(reviewReady, ctx.copy(analyticsEnabled = false)))
    }
}
