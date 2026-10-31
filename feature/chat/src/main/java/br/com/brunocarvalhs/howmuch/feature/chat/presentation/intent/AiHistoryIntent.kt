package br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent

data class AiHistoryIntent(
    val onSelectSession: (String) -> Unit = {},
    val onDeleteSession: (String) -> Unit = {},
    val onNewChat: () -> Unit = {},
    val onBack: () -> Unit = {}
)
