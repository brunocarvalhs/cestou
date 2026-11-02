package br.com.brunocarvalhs.howmuch.feature.shopping.data.text

import android.content.Context
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.NotificationTexts
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

internal class ResourceNotificationTexts @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationTexts {
    override fun justNow(): String = context.getString(R.string.notifications_time_now)

    override fun minutesAgo(minutes: Int): String =
        context.resources.getQuantityString(R.plurals.notifications_time_minutes_ago, minutes, minutes)

    override fun hoursAgo(hours: Int): String =
        context.resources.getQuantityString(R.plurals.notifications_time_hours_ago, hours, hours)

    /** Read the locale on each call: it can change while the process lives (per-app language). */
    override fun shortDate(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(context.resources.configuration.locales[0])
            .format(date)
}
