package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent

internal data class AiChatIntent(
    val onInputChange: (String) -> Unit = {},
    val onSendMessage: () -> Unit = {}
)
