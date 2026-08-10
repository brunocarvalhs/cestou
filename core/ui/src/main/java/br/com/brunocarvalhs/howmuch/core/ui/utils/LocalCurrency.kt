package br.com.brunocarvalhs.howmuch.core.ui.utils

import androidx.compose.runtime.compositionLocalOf
import java.util.Currency
import java.util.Locale

val LocalCurrency = compositionLocalOf { 
    try {
        Currency.getInstance(Locale.getDefault()).currencyCode
    } catch (_: Exception) {
        "BRL"
    }
}
