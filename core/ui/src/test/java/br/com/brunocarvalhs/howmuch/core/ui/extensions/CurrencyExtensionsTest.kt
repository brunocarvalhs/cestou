package br.com.brunocarvalhs.howmuch.core.ui.extensions

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrencyExtensionsTest {

    @Test
    fun `formatPrice should format double to currency string`() {
        Locale.setDefault(Locale("pt", "BR"))
        val price = PRICE
        val formatted = price.formatPrice("BRL")
        
        // Use contains because the non-breaking space and currency symbol can vary between environments
        assertTrue(formatted.contains("1.234,56"))
        assertTrue(formatted.contains("R$"))
    }

    @Test
    fun `formatPrice with USD should format correctly`() {
        Locale.setDefault(Locale.US)
        val price = PRICE
        val formatted = price.formatPrice("USD")
        
        assertTrue(formatted.contains("1,234.56"))
        assertTrue(formatted.contains("$"))
    }

    companion object {
        const val PRICE = 1234.56
    }
}
