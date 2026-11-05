package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R

internal enum class ShoppingFilter(val title: UiText) {
    ALL(UiText.StringResource(R.string.shopping_filter_all)),
    FAVORITES(UiText.StringResource(R.string.shopping_filter_favorites)),
    OPEN(UiText.StringResource(R.string.shopping_filter_open)),
    GROUP(UiText.StringResource(R.string.shopping_filter_group)),
    FINISHED(UiText.StringResource(R.string.shopping_filter_finished))
}
