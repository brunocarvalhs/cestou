package br.com.brunocarvalhs.howmuch.feature.settings.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Keeps the daily "pending shopping list" reminder in sync with the persisted [AppSettings]:
 * enqueues (or updates the schedule of) a periodic [ShoppingReminderWorker] when both the
 * notifications master switch and the shopping-specific reminder are on, and cancels it
 * otherwise. Call [sync] after every settings write that can affect either flag or the time.
 */
internal class ShoppingReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun sync(settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)

        if (!settings.notificationsEnabled || !settings.remindersEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<ShoppingReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis(settings.reminderTime), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun initialDelayMillis(reminderTime: String): Long {
        val (hour, minute) = parseTime(reminderTime)
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun parseTime(reminderTime: String): Pair<Int, Int> {
        val parts = reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: DEFAULT_HOUR
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: DEFAULT_MINUTE
        return hour to minute
    }

    companion object {
        const val WORK_NAME = "shopping_reminder"
        private const val DEFAULT_HOUR = 18
        private const val DEFAULT_MINUTE = 0
    }
}
