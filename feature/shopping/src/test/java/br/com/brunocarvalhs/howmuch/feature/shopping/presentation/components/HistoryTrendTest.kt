package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components

import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.fullLabel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.shortLabel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history.trendRatios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.util.Locale

class HistoryTrendTest {

    @Test
    fun `ratios are normalised by the largest total`() {
        assertEquals(listOf(0.5f, 1f, 0.25f), trendRatios(listOf(50.0, 100.0, 25.0)))
    }

    @Test
    fun `all zero totals sit on the base line`() {
        assertEquals(listOf(0f, 0f), trendRatios(listOf(0.0, 0.0)))
    }

    @Test
    fun `no totals no ratios`() {
        assertTrue(trendRatios(emptyList()).isEmpty())
    }

    @Test
    fun `a single month is the maximum`() {
        assertEquals(listOf(1f), trendRatios(listOf(42.0)))
    }

    @Test
    fun `month labels are localised`() {
        val nov = YearMonth.of(2026, 11)
        assertEquals("NOV", nov.shortLabel(Locale.forLanguageTag("pt-BR")))
        assertEquals("novembro", nov.fullLabel(Locale.forLanguageTag("pt-BR")))
        assertEquals("november", nov.fullLabel(Locale.ENGLISH))
    }
}
