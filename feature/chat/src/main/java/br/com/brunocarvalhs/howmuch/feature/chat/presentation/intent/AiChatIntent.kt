package br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent

internal data class AiChatIntent(
    val onInputChange: (String) -> Unit = {},
    val onSendMessage: () -> Unit = {}
)
