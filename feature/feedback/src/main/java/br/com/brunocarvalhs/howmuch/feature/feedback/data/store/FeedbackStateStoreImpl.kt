package br.com.brunocarvalhs.howmuch.feature.feedback.data.store

import android.content.Context
import android.content.SharedPreferences
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackState
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "feedback_state"
private const val KEY_FIRST_SEEN = "first_seen_at"
private const val KEY_SESSIONS = "sessions"
private const val KEY_PURCHASES = "purchases_finished"
private const val KEY_LAST_SHOWN = "last_shown_at"
private const val KEY_LAST_NPS = "last_nps_at"
private const val KEY_CSAT_PREFIX = "last_csat_at_"
private const val KEY_DISMISSALS = "consecutive_dismissals"
private const val KEY_LAST_DISMISSED = "last_dismissed_at"
private const val KEY_LAST_LOW_SCORE = "last_low_score_at"
private const val KEY_LAST_STORE_REVIEW = "last_store_review_at"

private const val NPS_LOW_MAX = 6
private const val CSAT_LOW_MAX = 2

/**
 * Guarda os carimbos da política em SharedPreferences, com espelho em memória lido uma vez: é uma
 * decisão de UI síncrona que não pode suspender (mesma escolha do `FirstTimeEventTracker`).
 */
@Singleton
internal class FeedbackStateStoreImpl internal constructor(
    private val prefs: SharedPreferences,
    private val nowMillis: () -> Long
) : FeedbackStateStore {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        System::currentTimeMillis
    )

    private var current: FeedbackState? = null

    override fun load(): FeedbackState = current ?: read().also { current = it }

    override fun recordSession() {
        val now = nowMillis()
        val s = load()
        update(s.copy(firstSeenAt = if (s.firstSeenAt > 0L) s.firstSeenAt else now, sessions = s.sessions + 1))
    }

    override fun recordPurchaseFinished() {
        val s = load()
        update(s.copy(purchasesFinished = s.purchasesFinished + 1))
    }

    override fun recordShown(prompt: FeedbackPrompt) {
        if (prompt.kind == SurveyKind.STORE_REVIEW) return
        update(load().copy(lastShownAt = nowMillis()))
    }

    override fun recordAnswered(prompt: FeedbackPrompt, score: Int) {
        val now = nowMillis()
        val s = load()
        val low = when (prompt.kind) {
            SurveyKind.NPS -> score <= NPS_LOW_MAX
            SurveyKind.CSAT -> score <= CSAT_LOW_MAX
            SurveyKind.STORE_REVIEW -> false
        }
        update(
            s.copy(
                lastNpsAt = if (prompt.kind == SurveyKind.NPS) now else s.lastNpsAt,
                lastCsatAtByFeature = csatStamps(s, prompt, now),
                consecutiveDismissals = 0,
                lastLowScoreAt = if (low) now else s.lastLowScoreAt
            )
        )
    }

    override fun recordDismissed(prompt: FeedbackPrompt) {
        val s = load()
        update(s.copy(consecutiveDismissals = s.consecutiveDismissals + 1, lastDismissedAt = nowMillis()))
    }

    override fun recordStoreReviewRequested() {
        update(load().copy(lastStoreReviewAt = nowMillis()))
    }

    private fun csatStamps(s: FeedbackState, prompt: FeedbackPrompt, now: Long): Map<String, Long> {
        val feature = prompt.feature
        return if (prompt.kind == SurveyKind.CSAT && feature != null) {
            s.lastCsatAtByFeature + (feature to now)
        } else {
            s.lastCsatAtByFeature
        }
    }

    private fun update(next: FeedbackState) {
        current = next
        prefs.edit().apply {
            putLong(KEY_FIRST_SEEN, next.firstSeenAt)
            putInt(KEY_SESSIONS, next.sessions)
            putInt(KEY_PURCHASES, next.purchasesFinished)
            putLong(KEY_LAST_SHOWN, next.lastShownAt)
            putLong(KEY_LAST_NPS, next.lastNpsAt)
            next.lastCsatAtByFeature.forEach { (feature, at) -> putLong(KEY_CSAT_PREFIX + feature, at) }
            putInt(KEY_DISMISSALS, next.consecutiveDismissals)
            putLong(KEY_LAST_DISMISSED, next.lastDismissedAt)
            putLong(KEY_LAST_LOW_SCORE, next.lastLowScoreAt)
            putLong(KEY_LAST_STORE_REVIEW, next.lastStoreReviewAt)
        }.apply()
    }

    private fun read() = FeedbackState(
        firstSeenAt = prefs.getLong(KEY_FIRST_SEEN, 0L),
        sessions = prefs.getInt(KEY_SESSIONS, 0),
        purchasesFinished = prefs.getInt(KEY_PURCHASES, 0),
        lastShownAt = prefs.getLong(KEY_LAST_SHOWN, 0L),
        lastNpsAt = prefs.getLong(KEY_LAST_NPS, 0L),
        lastCsatAtByFeature = prefs.all
            .filterKeys { it.startsWith(KEY_CSAT_PREFIX) }
            .mapNotNull { (key, value) -> (value as? Long)?.let { key.removePrefix(KEY_CSAT_PREFIX) to it } }
            .toMap(),
        consecutiveDismissals = prefs.getInt(KEY_DISMISSALS, 0),
        lastDismissedAt = prefs.getLong(KEY_LAST_DISMISSED, 0L),
        lastLowScoreAt = prefs.getLong(KEY_LAST_LOW_SCORE, 0L),
        lastStoreReviewAt = prefs.getLong(KEY_LAST_STORE_REVIEW, 0L)
    )
}
