package br.com.brunocarvalhs.howmuch.feature.shopping.domain.model

import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification

/** `type` values written by the notification producers (see `ShoppingJoinUseCase`, `FinishPurchaseViewModel`). */
internal object NotificationTypes {
    const val LIST_JOINED = "list_joined"
    const val LIST_FINISHED = "list_finished"
    const val CAMPAIGN = "campaign"

    val LIST_TYPES = setOf(LIST_JOINED, LIST_FINISHED)
}

internal enum class NotificationFilter { ALL, LISTS }

/** Declaration order is display order: most recent section first. */
internal enum class NotificationSectionKey { TODAY, YESTERDAY, EARLIER }

internal data class NotificationGroup(
    val key: NotificationSectionKey,
    val notifications: List<Notification>
)
