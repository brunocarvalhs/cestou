package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state

import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList

/**
 * Notificações no relógio: leitura da coleção `notifications`. [signedOut] quando não há
 * conta no relógio, então a tela mostra o vazio em vez de ficar carregando.
 */
data class WearNotificationsUiState(
    val items: StableList<Notification> = StableList(emptyList()),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val signedOut: Boolean = false
)
