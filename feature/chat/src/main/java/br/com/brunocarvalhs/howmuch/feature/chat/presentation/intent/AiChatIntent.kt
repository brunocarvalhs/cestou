package br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent

data class AiChatIntent(
    val onInputChange: (String) -> Unit = {},
    val onSendMessage: () -> Unit = {},
    val onSuggestionSelected: (id: String, text: String) -> Unit = { _, _ -> },
    val onSettings: () -> Unit = {},
    val onBack: () -> Unit = {}
)
