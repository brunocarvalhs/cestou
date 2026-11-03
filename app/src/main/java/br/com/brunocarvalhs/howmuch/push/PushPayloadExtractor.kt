package br.com.brunocarvalhs.howmuch.push

import android.content.Intent
import android.os.Bundle
import br.com.brunocarvalhs.howmuch.core.domain.model.PushPayload
import com.google.firebase.messaging.RemoteMessage

/**
 * Edge adapter: turns the two shapes FCM hands the app into the Android-free [PushPayload].
 *
 * A notification the system draws itself (app in the background or closed) never reaches
 * [FirebaseMessagingService.onMessageReceived]; tapping it launches the app with the message's
 * custom data and `google.*` bookkeeping keys as `Intent` extras, and without the notification
 * block's title/body. That is why campaign text travels in custom data.
 */
internal object PushPayloadExtractor {
    private const val KEY_MESSAGE_ID = "google.message_id"
    private const val KEY_SENT_TIME = "google.sent_time"
    private val RESERVED_PREFIXES = listOf("google.", "gcm.", "firebase.")
    private val RESERVED_KEYS = setOf("from", "collapse_key", "message_type")

    fun from(message: RemoteMessage): PushPayload = PushPayload(
        messageId = message.messageId,
        sentTimeMillis = message.sentTime.takeIf { it > 0 },
        notificationTitle = message.notification?.title,
        notificationBody = message.notification?.body,
        data = message.data
    )

    /** Null when [intent] is not the launch of an FCM tray notification (a normal start, an invite link...). */
    fun from(intent: Intent): PushPayload? {
        val extras = intent.extras
        val messageId = extras?.getString(KEY_MESSAGE_ID)
        val isFcm = extras != null && (messageId != null || extras.containsKey(KEY_SENT_TIME))
        return if (isFcm) {
            PushPayload(
                messageId = messageId,
                sentTimeMillis = extras.sentTime(),
                notificationTitle = null,
                notificationBody = null,
                data = extras.customData()
            )
        } else {
            null
        }
    }

    private fun Bundle.sentTime(): Long? {
        val value = getString(KEY_SENT_TIME)?.toLongOrNull() ?: getLong(KEY_SENT_TIME, 0L)
        return value.takeIf { it > 0 }
    }

    private fun Bundle.customData(): Map<String, String> = keySet()
        .filterNot { key -> key in RESERVED_KEYS || RESERVED_PREFIXES.any { key.startsWith(it) } }
        .mapNotNull { key -> getString(key)?.let { key to it } }
        .toMap()
}
