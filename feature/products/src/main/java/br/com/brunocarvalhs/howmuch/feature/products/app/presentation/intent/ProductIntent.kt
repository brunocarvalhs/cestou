package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent

sealed interface ProductIntent {
    data object FetchProducts : ProductIntent
    data object ClearError : ProductIntent
}
