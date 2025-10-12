package br.com.brunocarvalhs.howmuch.app.modules.token

sealed interface TokenUiIntent {
    data class SearchByToken(val token: String) : TokenUiIntent
}
