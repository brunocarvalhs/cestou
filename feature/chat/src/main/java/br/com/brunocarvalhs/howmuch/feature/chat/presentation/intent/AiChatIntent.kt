package br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent

data class AiChatIntent(
    val onInputChange: (String) -> Unit = {},
    val onSendMessage: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onToggleHistory: () -> Unit = {},
    val onSelectHistoryItem: (String) -> Unit = {},
    val onStartVoiceInput: () -> Unit = {},
    val onClose: () -> Unit = {}
)
