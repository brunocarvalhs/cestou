package br.com.brunocarvalhs.howmuch.app.modules.shared

sealed interface SharedCartUiIntent {
    data object SharedList : SharedCartUiIntent
    data object SharedToken : SharedCartUiIntent
}
