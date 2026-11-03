package br.com.brunocarvalhs.howmuch.feature.shopping.domain.text

import java.time.LocalDate

/** Relative-time wording for the notification feed, resolved in `data` so the domain stays free of Android. */
internal interface NotificationTexts {
    fun justNow(): String
    fun minutesAgo(minutes: Int): String
    fun hoursAgo(hours: Int): String
    fun shortDate(date: LocalDate): String
}
