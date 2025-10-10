package br.com.brunocarvalhs.howmuch.app.foundation.extensions

import java.util.Locale

private const val NUMBER = 100
private const val FORMAT = "%.2f"

fun Long.toCurrencyString(): String {
    val value = this.toDouble() / NUMBER
    return String.format(Locale.getDefault(), FORMAT, value).replace('.', ',')
}
