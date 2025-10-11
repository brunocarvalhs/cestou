package br.com.brunocarvalhs.howmuch.app.foundation.formatted
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

private const val NUMBER = 100
private const val NUMBER_FLOAT = 0.0
private const val REGEX = "[^0-9]"
private const val EMPTY = ""

class CurrencyVisualTransformation(
    locale: Locale = Locale.Builder()
        .setLanguage("pt")
        .setRegion("BR")
        .build()
) : VisualTransformation {

    private val formatter = NumberFormat.getCurrencyInstance(locale)

    override fun filter(text: AnnotatedString): TransformedText {
        val cleanText = text.text.replace(REGEX.toRegex(), EMPTY)
        val parsed = cleanText.toDoubleOrNull() ?: NUMBER_FLOAT
        val formatted = formatter.format(parsed / NUMBER)

        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = formatted.length
                override fun transformedToOriginal(offset: Int): Int = cleanText.length
            }
        )
    }
}
