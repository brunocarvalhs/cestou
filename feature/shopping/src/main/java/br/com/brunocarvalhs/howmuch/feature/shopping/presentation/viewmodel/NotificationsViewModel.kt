package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.NotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationType
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NotificationsViewModel @Inject constructor(
    private val authService: AuthService,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    var onBack: () -> Unit = {}

    init {
        observeNotifications()
    }

    val intent = NotificationsIntent(
        onNotificationClick = { markAsRead(it) },
        onBack = { onBack() }
    )

    private fun observeNotifications() {
        val userId = authService.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            notificationRepository.observeNotifications(userId).collect { list ->
                _uiState.update {
                    it.copy(
                        notifications = list.map { n ->
                            NotificationItem(
                                id = n.id,
                                title = n.title,
                                description = n.message,
                                time = "Há pouco", // Need real time mapping
                                type = try { NotificationType.valueOf(n.type) } catch (e: Exception) { NotificationType.FEATURE },
                                isRead = n.isRead
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }
}
