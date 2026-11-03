package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.NotificationTexts
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FormatNotificationTimeUseCaseTest {

    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-02T12:00:00Z")

    private val texts = object : NotificationTexts {
        override fun justNow() = "now"
        override fun minutesAgo(minutes: Int) = "${minutes}m"
        override fun hoursAgo(hours: Int) = "${hours}h"
        override fun shortDate(date: LocalDate) = "date:$date"
    }

    private val useCase = FormatNotificationTimeUseCase(Clock.fixed(now, zone), texts)

    private fun format(ago: Duration) = useCase(now.minus(ago).toEpochMilli())

    @Test
    fun `under a minute is just now`() {
        assertEquals("now", format(Duration.ofSeconds(59)))
    }

    @Test
    fun `a negative elapsed time from clock skew is just now`() {
        assertEquals("now", format(Duration.ofMinutes(-5)))
    }

    @Test
    fun `minutes up to the hour boundary`() {
        assertEquals("1m", format(Duration.ofMinutes(1)))
        assertEquals("59m", format(Duration.ofMinutes(59)))
    }

    @Test
    fun `hours up to the day boundary`() {
        assertEquals("1h", format(Duration.ofMinutes(60)))
        assertEquals("23h", format(Duration.ofHours(23).plusMinutes(59)))
    }

    @Test
    fun `a day or more falls back to the short date in the clock zone`() {
        assertEquals("date:2026-11-01", format(Duration.ofDays(1)))
        assertEquals("date:2026-10-03", format(Duration.ofDays(30)))
    }
}
