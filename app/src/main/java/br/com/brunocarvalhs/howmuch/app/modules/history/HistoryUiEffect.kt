package br.com.brunocarvalhs.howmuch.app.modules.history

sealed class HistoryUiEffect {
    data class ShowError(val message: String) : HistoryUiEffect()
}
