package br.com.brunocarvalhs.howmuch.core.ui.utils

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CurrencyVisualTransformationTest {

    private val transformation = CurrencyVisualTransformation(locale = Locale("pt", "BR"), currencyCode = "BRL")

    @Test
    fun `filter formats digit-only input as a currency amount with two decimals`() {
        val result = transformation.filter(AnnotatedString("12345"))

        assertEquals("123,45", result.text.text)
    }

    @Test
    fun `filter ignores non-digit characters before formatting`() {
        val result = transformation.filter(AnnotatedString("1a2b3c"))

        assertEquals("1,23", result.text.text)
    }

    @Test
    fun `filter treats empty input as zero`() {
        val result = transformation.filter(AnnotatedString(""))

        assertEquals("0,00", result.text.text)
    }

    @Test
    fun `offsetMapping maps to the ends of the transformed and original text`() {
        val original = AnnotatedString("12345")
        val result = transformation.filter(original)

        assertEquals(result.text.text.length, result.offsetMapping.originalToTransformed(0))
        assertEquals(original.length, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun `filter falls back gracefully when the currency code is invalid`() {
        val transformationWithInvalidCurrency =
            CurrencyVisualTransformation(locale = Locale("pt", "BR"), currencyCode = "NOT_A_CODE")

        val result = transformationWithInvalidCurrency.filter(AnnotatedString("100"))

        assertEquals("1,00", result.text.text)
    }
}
