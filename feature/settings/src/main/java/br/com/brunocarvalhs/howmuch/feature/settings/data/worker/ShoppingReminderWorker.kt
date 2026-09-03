package br.com.brunocarvalhs.howmuch.feature.settings.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.settings.R
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

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
    }

    override suspend fun doWork(): Result {
        val shoppingRepository = EntryPoints.get(
            applicationContext,
            Dependencies::class.java
        ).shoppingRepository()

        val pendingCount = try {
            shoppingRepository.getAll().count { it.status != Shopping.Status.FINISH }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to load shopping lists for the reminder")
            return Result.retry()
        }

        if (pendingCount > 0) {
            notifyPending(pendingCount)
        }
        return Result.success()
    }

    private fun notifyPending(pendingCount: Int) {
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
            return
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
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "ShoppingReminderWorker"
        const val CHANNEL_ID = "shopping_reminders"
        private const val NOTIFICATION_ID = 1001
    }
}
