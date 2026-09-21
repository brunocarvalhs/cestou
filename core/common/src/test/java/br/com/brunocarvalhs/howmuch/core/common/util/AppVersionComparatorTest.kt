package br.com.brunocarvalhs.howmuch.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionComparatorTest {

    @Test
    fun `compare returns 0 for equal versions`() {
        assertEquals(0, AppVersionComparator.compare("1.3.0", "1.3.0"))
    }

    @Test
    fun `compare returns positive when major is greater`() {
        assertTrue(AppVersionComparator.compare("2.0.0", "1.9.9") > 0)
    }

    @Test
    fun `compare returns negative when major is lower`() {
        assertTrue(AppVersionComparator.compare("1.9.9", "2.0.0") < 0)
    }

    @Test
    fun `compare falls back to minor when major is equal`() {
        assertTrue(AppVersionComparator.compare("1.4.0", "1.3.9") > 0)
    }

    @Test
    fun `compare falls back to patch when major and minor are equal`() {
        assertTrue(AppVersionComparator.compare("1.3.1", "1.3.0") > 0)
    }

    @Test
    fun `compare treats missing segments as zero`() {
        assertEquals(0, AppVersionComparator.compare("1.3", "1.3.0"))
        assertTrue(AppVersionComparator.compare("1.3.1", "1.3") > 0)
    }

    @Test
    fun `compare treats non-numeric segments as zero`() {
        assertEquals(0, AppVersionComparator.compare("1.3.0-debug", "1.3.debug"))
    }

    @Test
    fun `isAtLeastVersion is true when equal or greater`() {
        assertTrue("1.3.0".isAtLeastVersion("1.3.0"))
        assertTrue("1.4.0".isAtLeastVersion("1.3.0"))
        assertFalse("1.2.0".isAtLeastVersion("1.3.0"))
    }

    @Test
    fun `isAtMostVersion is true when equal or lower`() {
        assertTrue("1.3.0".isAtMostVersion("1.3.0"))
        assertTrue("1.2.0".isAtMostVersion("1.3.0"))
        assertFalse("1.4.0".isAtMostVersion("1.3.0"))
    }
}
