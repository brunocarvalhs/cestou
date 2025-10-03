package br.com.brunocarvalhs.howmuch.app.modules.menu

import androidx.annotation.StringRes

data class ServiceItem(
    @param:StringRes val title: Int,
    val onClick: () -> Unit
)
