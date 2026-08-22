package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state

import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R

internal enum class ShoppingFilter(val title: UiText) {
    ALL(UiText.StringResource(R.string.shopping_filter_all)),
    SHOPPING(UiText.StringResource(R.string.shopping_filter_active)),
    FAVORITES(UiText.StringResource(R.string.shopping_filter_favorites))
}
