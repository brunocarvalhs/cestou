package br.com.brunocarvalhs.howmuch.core.analytics.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsCatalogTest {

    private val eventValues = AnalyticsEvents::class.java.declaredFields
        .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
        .map { it.apply { isAccessible = true }.get(null) as String }

    @Test
    fun `event names are unique snake_case and fit the Firebase limit`() {
        assertEquals(eventValues.size, eventValues.toSet().size)
        eventValues.forEach { name ->
            assertTrue("invalid event name: $name", Regex("^[a-z][a-z0-9_]{0,39}$").matches(name))
        }
    }

    @Test
    fun `screen ids are unique and screen classes never name a view model`() {
        assertEquals(AnalyticsScreen.entries.size, AnalyticsScreen.entries.map { it.id }.toSet().size)
        AnalyticsScreen.entries.forEach {
            assertTrue("${it.screenClass} looks like a ViewModel", !it.screenClass.endsWith("ViewModel"))
        }
    }

    @Test
    fun `account age buckets cover every range`() {
        assertEquals("0-1d", AnalyticsUserProperties.accountAgeBucket(0))
        assertEquals("2-7d", AnalyticsUserProperties.accountAgeBucket(7))
        assertEquals("8-30d", AnalyticsUserProperties.accountAgeBucket(30))
        assertEquals("30d+", AnalyticsUserProperties.accountAgeBucket(31))
    }

    @Test
    fun `lists buckets cover every range`() {
        assertEquals("0", AnalyticsUserProperties.listsBucket(0))
        assertEquals("1-3", AnalyticsUserProperties.listsBucket(3))
        assertEquals("4+", AnalyticsUserProperties.listsBucket(4))
    }

    @Test
    fun `goal buckets cover every range without exposing the value`() {
        assertEquals("<500", AnalyticsBuckets.goal(499.99))
        assertEquals("500-1500", AnalyticsBuckets.goal(500.0))
        assertEquals("1500-3000", AnalyticsBuckets.goal(1500.0))
        assertEquals("3000+", AnalyticsBuckets.goal(3000.0))
    }

    @Test
    fun `nps classes follow the 0-6, 7-8 and 9-10 ranges`() {
        assertEquals("detractor", AnalyticsUserProperties.npsClass(0))
        assertEquals("detractor", AnalyticsUserProperties.npsClass(6))
        assertEquals("passive", AnalyticsUserProperties.npsClass(7))
        assertEquals("passive", AnalyticsUserProperties.npsClass(8))
        assertEquals("promoter", AnalyticsUserProperties.npsClass(9))
        assertEquals("promoter", AnalyticsUserProperties.npsClass(10))
    }
}
