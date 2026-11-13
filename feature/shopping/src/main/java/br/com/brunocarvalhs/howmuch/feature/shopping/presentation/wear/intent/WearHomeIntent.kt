package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent

internal sealed interface WearHomeIntent {
    /** "Criar lista no celular": abre o app do celular em criar lista. */
    data object CreateListOnPhone : WearHomeIntent

    /** "Mais" / "Menos": mostra ou oculta as listas além das 5 recentes. */
    data object ToggleShowAll : WearHomeIntent
}
