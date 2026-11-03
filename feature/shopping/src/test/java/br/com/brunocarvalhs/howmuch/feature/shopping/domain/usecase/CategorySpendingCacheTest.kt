package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySpendingCacheTest {

    private val cache = CategorySpendingCache()

    @Test
    fun `returns what was stored for the same key`() {
        cache.put("a", 1L, mapOf("carnes" to 10.0))

        assertEquals(mapOf("carnes" to 10.0), cache.get("a", 1L))
    }

    @Test
    fun `misses on another list or another finishedAt`() {
        cache.put("a", 1L, mapOf("carnes" to 10.0))

        assertNull(cache.get("b", 1L))
        assertNull(cache.get("a", 2L))
        assertNull(cache.get("a", null))
    }

    @Test
    fun `a lists without finishedAt is cached under null`() {
        cache.put("legacy", null, mapOf("x" to 1.0))

        assertEquals(mapOf("x" to 1.0), cache.get("legacy", null))
    }
}
