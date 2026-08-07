package br.com.brunocarvalhs.howmuch.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import br.com.brunocarvalhs.howmuch.core.ui.utils.CurrencyVisualTransformation
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import java.text.NumberFormat
import java.util.*

fun Double.formatPrice(currencyCode: String? = null): String {
    val format = NumberFormat.getCurrencyInstance()
    if (currencyCode != null) {
        try {
            format.currency = Currency.getInstance(currencyCode)
        } catch (_: Exception) {}
    }
    return format.format(this)
}

@Stable
class CurrencyFormatter(private val numberFormat: NumberFormat) {
    val currency: Currency? get() = try { numberFormat.currency } catch (_: Exception) { null }
    fun format(amount: Double): String = numberFormat.format(amount)
}

@Composable
fun rememberCurrencyFormatter(): CurrencyFormatter {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val format = NumberFormat.getCurrencyInstance()
        CurrencyFormatter(format)
    }
}

@Composable
fun rememberCurrencyVisualTransformation(): CurrencyVisualTransformation {
    val currencyCode = LocalCurrency.current
    val configuration = LocalConfiguration.current
    return remember(currencyCode, configuration) {
        val locale = configuration.locales[0]
        CurrencyVisualTransformation(locale, currencyCode)
    }
}
