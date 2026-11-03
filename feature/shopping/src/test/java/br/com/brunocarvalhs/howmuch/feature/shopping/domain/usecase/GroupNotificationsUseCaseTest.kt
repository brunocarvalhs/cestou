package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationSectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class GroupNotificationsUseCaseTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = ZonedDateTime.of(2026, 11, 2, 10, 0, 0, 0, zone)
    private val useCase = GroupNotificationsUseCase(Clock.fixed(now.toInstant(), zone))

    private fun notification(id: String, at: ZonedDateTime, type: String = "list_joined") = Notification(
        id = id,
        userId = "u1",
        title = "t",
        message = "m",
        type = type,
        isRead = false,
        timestamp = at.toInstant().toEpochMilli()
    )

    @Test
    fun `buckets by calendar day, newest first, and keeps section order`() {
        val groups = useCase(
            listOf(
                notification("old", now.minusDays(5)),
                notification("today-early", now.withHour(1)),
                notification("yesterday", now.minusDays(1)),
                notification("today-late", now.withHour(9))
            ),
            NotificationFilter.ALL
        )

        assertEquals(
            listOf(NotificationSectionKey.TODAY, NotificationSectionKey.YESTERDAY, NotificationSectionKey.EARLIER),
            groups.map { it.key }
        )
        assertEquals(listOf("today-late", "today-early"), groups[0].notifications.map { it.id })
        assertEquals(listOf("yesterday"), groups[1].notifications.map { it.id })
        assertEquals(listOf("old"), groups[2].notifications.map { it.id })
    }

    @Test
    fun `a notification just before midnight belongs to yesterday and just after to today`() {
        val midnight = now.toLocalDate().atStartOfDay(zone)
        val groups = useCase(
            listOf(notification("before", midnight.minusSeconds(1)), notification("after", midnight)),
            NotificationFilter.ALL
        )

        assertEquals(
            listOf("after"),
            groups.single { it.key == NotificationSectionKey.TODAY }.notifications.map { it.id }
        )
        assertEquals(
            listOf("before"),
            groups.single { it.key == NotificationSectionKey.YESTERDAY }.notifications.map { it.id }
        )
    }

    @Test
    fun `omits empty sections`() {
        val groups = useCase(listOf(notification("a", now.minusMinutes(3))), NotificationFilter.ALL)

        assertEquals(listOf(NotificationSectionKey.TODAY), groups.map { it.key })
    }

    @Test
    fun `a timestamp in the future still lands in today`() {
        val groups = useCase(listOf(notification("future", now.plusDays(2))), NotificationFilter.ALL)

        assertEquals(NotificationSectionKey.TODAY, groups.single().key)
    }

    @Test
    fun `lists filter keeps only list notifications`() {
        val groups = useCase(
            listOf(
                notification("joined", now, "list_joined"),
                notification("finished", now.minusMinutes(1), "list_finished"),
                notification("other", now.minusMinutes(2), "promo")
            ),
            NotificationFilter.LISTS
        )

        assertEquals(listOf("joined", "finished"), groups.single().notifications.map { it.id })
    }

    @Test
    fun `all filter keeps unknown types`() {
        val groups = useCase(listOf(notification("other", now, "promo")), NotificationFilter.ALL)

        assertEquals(listOf("other"), groups.single().notifications.map { it.id })
    }

    @Test
    fun `empty input yields no sections`() {
        assertTrue(useCase(emptyList(), NotificationFilter.ALL).isEmpty())
    }

    @Test
    fun `caps the feed to the most recent notifications`() {
        val many = (1..150).map { notification("n$it", now.minusMinutes(it.toLong())) }

        val groups = useCase(many, NotificationFilter.ALL)

        val kept = groups.sumOf { it.notifications.size }
        assertEquals(GroupNotificationsUseCase.MAX_NOTIFICATIONS, kept)
        assertEquals("n1", groups.first().notifications.first().id)
    }

    @Test
    fun `uses the clock zone to decide the day`() {
        val utc = ZoneId.of("UTC")
        val utcNow = Instant.parse("2026-11-02T01:00:00Z")
        val inUtc = GroupNotificationsUseCase(Clock.fixed(utcNow, utc))
        // 2026-11-01T23:00-03:00 == 02:00Z on Nov 2: same UTC day as "now", previous day in São Paulo.
        val at = ZonedDateTime.of(2026, 11, 1, 23, 0, 0, 0, zone)

        val groups = inUtc(listOf(notification("x", at)), NotificationFilter.ALL)

        assertEquals(NotificationSectionKey.TODAY, groups.single().key)
    }
}
