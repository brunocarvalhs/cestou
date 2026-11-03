package br.com.brunocarvalhs.howmuch.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import br.com.brunocarvalhs.howmuch.R

/**
 * The channel FCM uses for the notifications it draws itself (`default_notification_channel_id`
 * in the manifest). It must exist before the first campaign arrives, so it is created at startup.
 */
internal object PushChannel {
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                context.getString(R.string.push_channel_id),
                context.getString(R.string.push_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
}
