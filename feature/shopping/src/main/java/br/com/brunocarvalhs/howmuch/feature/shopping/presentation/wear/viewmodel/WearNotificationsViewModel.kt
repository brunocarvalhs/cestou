package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.MarkAllNotificationsReadUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearNotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearNotificationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notificações no relógio. Lê a mesma coleção do celular, mostra as mais recentes e
 * marca as não lidas. Tocar num aviso não abre a lista.
 */
@HiltViewModel
internal class WearNotificationsViewModel @Inject constructor(
    private val authService: AuthService,
    private val notificationRepository: NotificationRepository,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearNotificationsUiState())
    val uiState: StateFlow<WearNotificationsUiState> = _uiState.asStateFlow()

    private var unread: List<Notification> = emptyList()

    init {
        analytics.trackEvent(AnalyticsEvents.WEAR_NOTIFICATIONS_OPEN)
        val userId = authService.currentUser?.id
        if (userId == null) {
            _uiState.update { it.copy(isLoading = false, signedOut = true) }
        } else {
            viewModelScope.launch {
                notificationRepository.observeUnreadCount(userId).collect { count ->
                    _uiState.update { it.copy(unreadCount = count) }
                }
            }
            viewModelScope.launch {
                notificationRepository.observeNotifications(userId).collect { all -> publish(all) }
            }
        }
    }

    fun onIntent(intent: WearNotificationsIntent) {
        when (intent) {
            WearNotificationsIntent.MarkAllRead -> markAllRead()
        }
    }

    private fun publish(all: List<Notification>) {
        unread = all.filterNot { it.isRead }
        _uiState.update { it.copy(items = StableList(all.take(MAX_ROWS)), isLoading = false) }
    }

    private fun markAllRead() {
        if (unread.isEmpty()) return
        viewModelScope.launch { markAllNotificationsRead(unread) }
    }

    private companion object {
        /** Teto da lista no relógio: a tela pequena não lista o histórico inteiro. */
        const val MAX_ROWS = 30
    }
}
