package br.com.brunocarvalhs.howmuch.push

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.PushPayload
import br.com.brunocarvalhs.howmuch.core.domain.model.PushSaveOutcome
import br.com.brunocarvalhs.howmuch.core.domain.usecase.SavePushNotificationUseCase
import timber.log.Timber
import javax.inject.Inject

/** Where a push reached the app. Tracked, so the two FCM paths can be compared. */
internal enum class PushSource(val label: String) {
    /** `FirebaseMessagingService.onMessageReceived`. */
    MESSAGE("message"),

    /** The launch `Intent` of a tapped tray notification. */
    TAP("tap")
}

/**
 * Shared by the messaging service and the activity intent path: saves the push and reports what
 * happened. Never logs or tracks the campaign text (AGENTS.md section 5).
 */
internal class PushNotificationHandler @Inject constructor(
    private val savePushNotification: SavePushNotificationUseCase,
    private val analyticsTracker: AnalyticsTracker
) {
    suspend fun handle(payload: PushPayload, source: PushSource): PushSaveOutcome {
        analyticsTracker.trackEvent(AnalyticsEvents.PUSH_RECEIVED, mapOf(AnalyticsParams.SOURCE to source.label))
        val outcome = savePushNotification(payload)
        when (outcome) {
            PushSaveOutcome.Saved -> analyticsTracker.trackEvent(
                AnalyticsEvents.PUSH_SAVED,
                mapOf(AnalyticsParams.SOURCE to source.label)
            )
            PushSaveOutcome.AlreadySaved -> Timber.d("Push already saved")
            is PushSaveOutcome.Ignored -> analyticsTracker.trackEvent(
                AnalyticsEvents.PUSH_IGNORED,
                mapOf(AnalyticsParams.REASON to outcome.reason.name.lowercase())
            )
            is PushSaveOutcome.Failed -> {
                Timber.e(outcome.cause, "Failed to save a received push")
                analyticsTracker.trackEvent(
                    AnalyticsEvents.PUSH_SAVE_FAILED,
                    mapOf(AnalyticsParams.SOURCE to source.label)
                )
            }
        }
        return outcome
    }
}
