package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** "NOV" — abbreviated and uppercased for the month cards. */
internal fun YearMonth.shortLabel(locale: Locale = Locale.getDefault()): String =
    month.getDisplayName(TextStyle.SHORT_STANDALONE, locale).trimEnd('.').uppercase(locale)

/** "novembro" — lowercase on purpose, it is embedded in sentences such as "Compras de novembro". */
internal fun YearMonth.fullLabel(locale: Locale = Locale.getDefault()): String =
    month.getDisplayName(TextStyle.FULL_STANDALONE, locale).lowercase(locale)

internal fun Long.dayLabel(locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("dd", locale).format(Instant.ofEpochMilli(this).atZone(zone))

internal fun Long.shortMonthLabel(
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault()
): String = YearMonth.from(Instant.ofEpochMilli(this).atZone(zone)).shortLabel(locale)
