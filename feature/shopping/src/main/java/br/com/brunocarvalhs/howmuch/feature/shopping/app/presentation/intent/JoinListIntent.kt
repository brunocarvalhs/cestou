package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent

internal data class JoinListIntent(
    val onJoinByToken: (String) -> Unit = {},
    val onScanQrCode: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)
