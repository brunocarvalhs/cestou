package br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent

data class AiChatIntent(
    val onInputChange: (String) -> Unit = {},
    val onSendMessage: () -> Unit = {}
)
