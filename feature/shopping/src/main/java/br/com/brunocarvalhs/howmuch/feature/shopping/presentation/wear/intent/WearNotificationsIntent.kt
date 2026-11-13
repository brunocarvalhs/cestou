package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent

/** Ações da tela de notificações do relógio. Leitura, mais "marcar lidas". */
sealed interface WearNotificationsIntent {
    data object MarkAllRead : WearNotificationsIntent
}
