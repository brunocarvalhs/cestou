package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent

internal sealed interface WearSearchIntent {
    data class QueryChanged(val text: String) : WearSearchIntent
}
