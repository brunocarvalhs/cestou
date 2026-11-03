package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.NotificationTexts
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** Relative time for a notification: "just now", "N min ago", "N h ago", then a short date. */
internal class FormatNotificationTimeUseCase @Inject constructor(
    private val clock: Clock,
    private val texts: NotificationTexts
) {
    operator fun invoke(timestamp: Long): String {
        val then = Instant.ofEpochMilli(timestamp)
        val elapsed = Duration.between(then, clock.instant())
        return when {
            // A negative duration is clock skew between devices: show it as fresh instead of "in the future".
            elapsed < Duration.ofMinutes(1) -> texts.justNow()
            elapsed < Duration.ofHours(1) -> texts.minutesAgo(elapsed.toMinutes().toInt())
            elapsed < Duration.ofDays(1) -> texts.hoursAgo(elapsed.toHours().toInt())
            else -> texts.shortDate(then.atZone(clock.zone).toLocalDate())
        }
    }
}
