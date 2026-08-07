package br.com.brunocarvalhs.howmuch.core.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

class CurrencyVisualTransformation(
    private val locale: Locale = Locale.getDefault(),
    private val currencyCode: String = "BRL"
) : VisualTransformation {

    private companion object {
        const val CURRENCY_DIVISOR = 100.0
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        
        val number = if (digits.isEmpty()) 0.0 else digits.toDouble() / CURRENCY_DIVISOR
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            try {
                currency = java.util.Currency.getInstance(currencyCode)
            } catch (_: Exception) {}
        }
        
        val formatted = format.format(number)
            .replace(format.currency?.symbol ?: "", "")
            .trim()

        val annotatedString = AnnotatedString(formatted)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return text.length
            }
        }

        return TransformedText(annotatedString, offsetMapping)
    }
}
