package br.com.brunocarvalhs.howmuch.app.modules.shared


sealed interface SharedCartUiIntent {
    data class SharedList(val id: String) : SharedCartUiIntent
    data class SharedToken(val id: String) : SharedCartUiIntent
}