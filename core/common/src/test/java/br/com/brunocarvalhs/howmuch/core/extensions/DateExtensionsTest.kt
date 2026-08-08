package br.com.brunocarvalhs.howmuch.core.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DateExtensionsTest {

    @Test
    fun `toMonthYearString should format timestamp correctly`() {
        // Mock default locale to ensure consistent results
        Locale.setDefault(Locale.US)
        
        // 2026-08-08 12:00:00 UTC (approximate)
        val timestamp = 1786190400000L 
        
        val result = timestamp.toMonthYearString()
        
        // Note: The actual result might vary slightly based on the system timezone where the test runs
        // because of Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        // But for August 2026, it should be "August 2026"
        assertEquals("August 2026", result)
    }
}
