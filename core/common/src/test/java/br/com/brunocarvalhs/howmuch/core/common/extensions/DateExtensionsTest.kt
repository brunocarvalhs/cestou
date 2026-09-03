package br.com.brunocarvalhs.howmuch.core.common.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DateExtensionsTest {

    companion object {
        private const val AUGUST_2026_TIMESTAMP = 1786190400000L // 2026-08-08 12:00:00 UTC (approximate)
    }

    @Test
    fun `toMonthYearString should format timestamp correctly`() {
        // Mock default locale to ensure consistent results
        Locale.setDefault(Locale.US)
        
        val result = AUGUST_2026_TIMESTAMP.toMonthYearString()
        
        // Note: The actual result might vary slightly based on the system timezone where the test runs
        // because of Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        // But for August 2026, it should be "August 2026"
        assertEquals("August 2026", result)
    }
}
