package br.com.brunocarvalhs.howmuch.feature.settings.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.settings.R
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Fetched via [EntryPoints] instead of `@HiltWorker`/`HiltWorkerFactory` so this Worker can be
 * instantiated by WorkManager's default factory, without opting the app out of WorkManager's
 * on-demand initialization (which `@HiltWorker` would require via a custom
 * `Configuration.Provider`).
 */
internal class ShoppingReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Dependencies {
        fun shoppingRepository(): ShoppingRepository
        fun analyticsTracker(): AnalyticsTracker
    }

    override suspend fun doWork(): Result {
        val dependencies = EntryPoints.get(applicationContext, Dependencies::class.java)
        val shoppingRepository = dependencies.shoppingRepository()

        val pendingCount = try {
            shoppingRepository.getAll().count { it.status != Shopping.Status.FINISH }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to load shopping lists for the reminder")
            return Result.retry()
        }

        val today = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        if (shouldNotify(pendingCount, lastOpenDay(), today) && notifyPending(pendingCount)) {
            dependencies.analyticsTracker().trackEvent(
                AnalyticsEvents.REMINDER_SHOWN,
                mapOf(AnalyticsParams.PENDING_COUNT to pendingCount)
            )
        }
        return Result.success()
    }

    /**
     * Day of the last app open, as written by the app's `AppUsageTracker` (same prefs file and key).
     * Read here by name because `:feature:settings` doesn't depend on `:app`; -1 when unknown.
     */
    private fun lastOpenDay(): Long = applicationContext
        .getSharedPreferences(USAGE_PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(KEY_LAST_OPEN_DAY, -1L)

    /** Returns true when the notification was posted. */
    private fun notifyPending(pendingCount: Int): Boolean {
        val context = applicationContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.settings_shopping_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.tag(TAG).d("Skipping reminder: POST_NOTIFICATIONS not granted")
            return false
        }

        // Tapping the reminder opens the app on its list screen (sdd-061).
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // TODO: swap for a dedicated monochrome notification icon asset.
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.settings_shopping_reminder_notification_title))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.settings_shopping_reminder_notification_text,
                    pendingCount,
                    pendingCount
                )
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    companion object {
        private const val TAG = "ShoppingReminderWorker"
        const val CHANNEL_ID = "shopping_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val USAGE_PREFS_NAME = "analytics_usage"
        private const val KEY_LAST_OPEN_DAY = "last_open_day"

        /**
         * Reminds only when a list is still open and the user hasn't opened the app today, so the
         * default-on reminder never nags someone who is already using the app (sdd-061).
         */
        internal fun shouldNotify(pendingCount: Int, lastOpenDay: Long, today: Long): Boolean =
            pendingCount > 0 && lastOpenDay < today
    }
}
