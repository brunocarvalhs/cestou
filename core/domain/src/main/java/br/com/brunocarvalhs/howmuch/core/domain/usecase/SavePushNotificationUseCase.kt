package br.com.brunocarvalhs.howmuch.core.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.PushPayload
import br.com.brunocarvalhs.howmuch.core.domain.model.PushSaveOutcome
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SaveResult
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Turns a received campaign push into a feed notification: reads the campaign fields, completes
 * what FCM does not carry (recipient, id, read flag, type, timestamp) and saves it.
 *
 * Campaign authoring contract (custom data in the Firebase console): `title` and `message`, both
 * required. In the foreground the notification block's title/body are accepted as a fallback.
 */
class SavePushNotificationUseCase @Inject constructor(
    private val authService: AuthService,
    private val notificationRepository: NotificationRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(payload: PushPayload): PushSaveOutcome {
        val userId = authService.currentUser?.id
        val title = (payload.data[KEY_TITLE] ?: payload.notificationTitle).clean(MAX_TITLE)
        val message = (payload.data[KEY_MESSAGE] ?: payload.notificationBody).clean(MAX_MESSAGE)
        return when {
            userId == null -> PushSaveOutcome.Ignored(PushSaveOutcome.IgnoreReason.NO_USER)
            title == null || message == null -> PushSaveOutcome.Ignored(PushSaveOutcome.IgnoreReason.EMPTY)
            else -> save(
                Notification(
                    id = idFor(payload.messageId),
                    userId = userId,
                    title = title,
                    message = message,
                    type = TYPE_CAMPAIGN,
                    isRead = false,
                    timestamp = payload.sentTimeMillis ?: clock.millis()
                )
            )
        }
    }

    private suspend fun save(notification: Notification): PushSaveOutcome =
        notificationRepository.save(notification).fold(
            onSuccess = { if (it == SaveResult.Saved) PushSaveOutcome.Saved else PushSaveOutcome.AlreadySaved },
            onFailure = { PushSaveOutcome.Failed(it) }
        )

    /** Blank counts as missing; the cap matches the Firestore rules, so an oversized text is cut, not refused. */
    private fun String?.clean(max: Int): String? = this?.trim()?.takeIf { it.isNotEmpty() }?.take(max)

    /**
     * Same FCM message, same document: the foreground and the tap paths (and any redelivery) collapse
     * into one. Characters a document id cannot hold are replaced; a message without an id cannot be
     * deduplicated and gets a random one.
     */
    private fun idFor(messageId: String?): String {
        val key = messageId?.takeIf { it.isNotBlank() }
            ?.replace(ID_FORBIDDEN, "_")
            ?.take(MAX_ID_KEY)
            ?: UUID.randomUUID().toString()
        return "$ID_PREFIX$key"
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val TYPE_CAMPAIGN = "campaign"
        private const val ID_PREFIX = "fcm-"
        private const val MAX_ID_KEY = 100
        private const val MAX_TITLE = 200
        private const val MAX_MESSAGE = 500
        private val ID_FORBIDDEN = Regex("[^A-Za-z0-9_-]")
    }
}
