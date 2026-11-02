package br.com.brunocarvalhs.howmuch.feature.settings.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.brunocarvalhs.howmuch.core.common.util.ReminderTimeParser
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.services.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Keeps the daily "pending shopping list" reminder in sync with the persisted [AppSettings]:
 * enqueues (or updates the schedule of) a periodic [ShoppingReminderWorker] when both the
 * notifications master switch and the shopping-specific reminder are on, and cancels it
 * otherwise. Call [sync] after every settings write that can affect either flag or the time.
 * Implements [ReminderScheduler] (`core/domain`) so `core/data`'s `SettingsRepositoryImpl` can
 * trigger it without depending on `feature/settings`.
 */
internal class ShoppingReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    override fun sync(settings: AppSettings) {
        /**
         * WorkManager can be unavailable here if its defensive manual init
         * (CestouApplication.onCreate) failed on this device - see the Crashlytics fatal on
         * JobScheduler.forNamespace this guards against. Skipping the sync keeps settings writes
         * working even though the reminder itself can't be scheduled.
         */
        val workManager = try {
            WorkManager.getInstance(context)
        } catch (_: IllegalStateException) {
            return
        }

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
        val (hour, minute) = ReminderTimeParser.parse(reminderTime)
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

    companion object {
        const val WORK_NAME = "shopping_reminder"
    }
}
