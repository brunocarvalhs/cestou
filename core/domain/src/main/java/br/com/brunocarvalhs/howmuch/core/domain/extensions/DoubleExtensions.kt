package br.com.brunocarvalhs.howmuch.core.domain.extensions

fun Double?.orEmpty(): Double = this ?: 0.0