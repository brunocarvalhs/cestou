package br.com.brunocarvalhs.howmuch.core.domain.model

/**
 * Raw content of one FCM message, free of Android types so it can cross into the domain. Built at
 * the edge (`RemoteMessage` while the app is in the foreground, the launch `Intent` extras after
 * the user taps the tray notification).
 *
 * [notificationTitle] and [notificationBody] only exist for the foreground path: a notification
 * the system draws itself hands the app the custom [data] pairs and nothing else.
 */
data class PushPayload(
    val messageId: String?,
    val sentTimeMillis: Long?,
    val notificationTitle: String?,
    val notificationBody: String?,
    val data: Map<String, String>
)
