package br.com.brunocarvalhs.howmuch.feature.products.presentation.intent

internal sealed interface ProductIntent {
    data object FetchProducts : ProductIntent
    data object ClearError : ProductIntent
}
