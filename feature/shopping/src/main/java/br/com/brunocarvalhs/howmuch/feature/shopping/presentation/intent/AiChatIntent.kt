package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

internal data class AiChatIntent(
    val onInputChange: (String) -> Unit = {},
    val onSendMessage: () -> Unit = {}
)
