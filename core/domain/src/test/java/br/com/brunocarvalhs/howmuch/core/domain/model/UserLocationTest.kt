package br.com.brunocarvalhs.howmuch.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserLocationTest {

    @Test
    fun `key ignores case and surrounding spaces`() {
        assertEquals(UserLocation("rj", " Niterói ").key, UserLocation("RJ", "niterói").key)
    }

    @Test
    fun `lists the 27 federative units`() {
        assertEquals(27, UserLocation.BRAZILIAN_STATES.distinct().size)
    }
}
