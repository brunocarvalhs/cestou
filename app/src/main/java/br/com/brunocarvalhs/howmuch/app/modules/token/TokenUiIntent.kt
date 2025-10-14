package br.com.brunocarvalhs.howmuch.app.modules.token

sealed interface TokenUiIntent {
    data object SearchByToken : TokenUiIntent
}
