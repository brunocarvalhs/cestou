package br.com.brunocarvalhs.howmuch.core.ui.utils

import androidx.compose.runtime.Immutable

@Immutable
data class StableList<T>(
    val items: List<T> = emptyList()
) : List<T> by items
