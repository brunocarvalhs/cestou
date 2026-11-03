package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationGroup
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationSectionKey
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationTypes
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Filters, orders (newest first) and buckets notifications into Today / Yesterday / Earlier by the
 * device's calendar day. Sorting happens here because the Firestore query has no `orderBy` (it would
 * need a composite index), and the feed is capped so a long-lived account does not render unbounded rows.
 */
internal class GroupNotificationsUseCase @Inject constructor(
    private val clock: Clock
) {
    operator fun invoke(
        notifications: List<Notification>,
        filter: NotificationFilter
    ): List<NotificationGroup> {
        val today = LocalDate.now(clock)
        val byKey = notifications
            .filter { filter == NotificationFilter.ALL || it.type in NotificationTypes.LIST_TYPES }
            .sortedByDescending { it.timestamp }
            .take(MAX_NOTIFICATIONS)
            .groupBy { sectionOf(it.timestamp, today) }
        return NotificationSectionKey.entries.mapNotNull { key ->
            byKey[key]?.let { NotificationGroup(key, it) }
        }
    }

    private fun sectionOf(timestamp: Long, today: LocalDate): NotificationSectionKey {
        val day = Instant.ofEpochMilli(timestamp).atZone(clock.zone).toLocalDate()
        return when {
            !day.isBefore(today) -> NotificationSectionKey.TODAY
            day == today.minusDays(1) -> NotificationSectionKey.YESTERDAY
            else -> NotificationSectionKey.EARLIER
        }
    }

    companion object {
        const val MAX_NOTIFICATIONS = 100
    }
}
