package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationTypes
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.FormatNotificationTimeUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GroupNotificationsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.MarkAllNotificationsReadUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetByIdUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.NotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationSection
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationType
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
internal class NotificationsViewModel @Inject constructor(
    private val authService: AuthService,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val shoppingGetByIdUseCase: ShoppingGetByIdUseCase,
    private val groupNotifications: GroupNotificationsUseCase,
    private val formatNotificationTime: FormatNotificationTimeUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    var onBack: () -> Unit = {}
    var onOpenShopping: (Shopping) -> Unit = {}

    /** Latest snapshot from the listener; the filter and the grouping are derived from it. */
    private var notifications: List<Notification> = emptyList()
    private val requestedSenders = mutableSetOf<String>()

    /**
     * What the screen is showing right now (selected filter and the feed cap applied). "Mark all
     * read" acts on this, not on [notifications], so it never touches rows the user cannot see.
     */
    private var visibleNotifications: List<Notification> = emptyList()
    private var lastClickAt: Long? = null

    val intent = NotificationsIntent(
        onNotificationClick = { openNotification(it) },
        onFilter = { filter ->
            analyticsTracker.trackAction(AnalyticsElements.NOTIFICATIONS_FILTER)
            _uiState.update { it.copy(filter = filter) }
            publish()
        },
        onMarkAllRead = { markAllRead() },
        onErrorShown = { _uiState.update { it.copy(error = null) } },
        onRefreshTimes = { publish() },
        onBack = { onBack() }
    )

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        val userId = authService.currentUser?.id
        if (userId == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            notificationRepository.observeNotifications(userId).collect { list ->
                notifications = list
                resolveSenderProfiles(list)
                publish()
            }
        }
    }

    /**
     * Rebuilds the sections from the latest snapshot and the selected filter. The relative time is
     * computed here, so it is as fresh as the last emission or filter change, not live-ticking.
     */
    private fun publish() {
        val groups = groupNotifications(notifications, _uiState.value.filter)
        visibleNotifications = groups.flatMap { it.notifications }
        _uiState.update { state ->
            state.copy(
                sections = StableList(
                    groups.map { group ->
                        NotificationSection(group.key, StableList(group.notifications.map(::toItem)))
                    }
                ),
                hasUnread = visibleNotifications.any { !it.isRead },
                isLoading = false
            )
        }
    }

    private fun toItem(notification: Notification) = NotificationItem(
        id = notification.id,
        title = notification.title,
        description = notification.message,
        relativeTime = formatNotificationTime(notification.timestamp),
        type = typeOf(notification.type),
        isRead = notification.isRead,
        shoppingId = notification.shoppingId,
        senderId = notification.senderId
    )

    private fun typeOf(type: String) = when (type) {
        NotificationTypes.LIST_JOINED -> NotificationType.LIST_JOINED
        NotificationTypes.LIST_FINISHED -> NotificationType.LIST_FINISHED
        else -> NotificationType.OTHER
    }

    /** One lookup per distinct sender, never per row; ids already requested are skipped on later emissions. */
    private fun resolveSenderProfiles(list: List<Notification>) {
        val newIds = list.mapNotNull { it.senderId }.toSet() - requestedSenders
        requestedSenders += newIds
        newIds.forEach { id ->
            viewModelScope.launch {
                userRepository.getUserProfile(id).firstOrNull()?.let { profile ->
                    _uiState.update { it.copy(senderProfiles = it.senderProfiles + (id to profile)) }
                }
            }
        }
    }

    /** A second tap during the list lookup or the navigation transition would open the cart twice. */
    private fun isDebouncedClick(): Boolean {
        val now = clock.millis()
        val ignored = lastClickAt?.let { now - it < CLICK_DEBOUNCE_MS } ?: false
        if (!ignored) lastClickAt = now
        return ignored
    }

    private fun openNotification(id: String) {
        val notification = notifications.firstOrNull { it.id == id } ?: return
        if (isDebouncedClick()) return
        analyticsTracker.trackEvent(
            AnalyticsEvents.NOTIFICATION_CLICKED,
            mapOf(AnalyticsParams.NOTIFICATION_TYPE to typeOf(notification.type).name.lowercase())
        )
        viewModelScope.launch {
            if (!notification.isRead) {
                notificationRepository.markAsRead(id).onFailure {
                    Timber.e(it, "Failed to mark notification as read")
                    showError(R.string.notifications_error_mark_read)
                }
            }
            val shoppingId = notification.shoppingId ?: return@launch
            shoppingGetByIdUseCase(shoppingId)
                .onSuccess { onOpenShopping(it) }
                .onFailure {
                    Timber.w(it, "Notification list unavailable")
                    showError(R.string.notifications_error_list_unavailable)
                }
        }
    }

    private fun markAllRead() {
        viewModelScope.launch {
            markAllNotificationsRead(visibleNotifications)
                .onSuccess { count ->
                    if (count > 0) {
                        analyticsTracker.trackEvent(
                            AnalyticsEvents.NOTIFICATIONS_MARK_ALL_READ,
                            mapOf(AnalyticsParams.ITEMS_COUNT to count)
                        )
                    }
                }
                .onFailure {
                    Timber.e(it, "Failed to mark all notifications as read")
                    showError(R.string.notifications_error_mark_read)
                }
        }
    }

    private fun showError(resId: Int) {
        _uiState.update { it.copy(error = UiText.StringResource(resId)) }
    }

    private companion object {
        const val CLICK_DEBOUNCE_MS = 1_000L
    }
}
