package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

internal data class JoinListIntent(
    val onJoinByToken: (String) -> Unit = {},
    val onScanQrCode: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)
