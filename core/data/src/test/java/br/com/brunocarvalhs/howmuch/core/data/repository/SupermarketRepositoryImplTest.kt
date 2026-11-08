package br.com.brunocarvalhs.howmuch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SupermarketRepositoryImplTest {

    private val dataStore = mockk<DataStore<Preferences>>()
    private val repository = SupermarketRepositoryImpl(dataStore)

    @Test
    fun `location is null until both state and city are stored`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())
        assertNull(repository.observeLocation().first())

        every { dataStore.data } returns flowOf(mutablePreferencesOf(stringPreferencesKey("supermarket_state") to "RJ"))
        assertNull(repository.observeLocation().first())
    }

    @Test
    fun `location is read from the stored state and city`() = runTest {
        every { dataStore.data } returns flowOf(
            mutablePreferencesOf(
                stringPreferencesKey("supermarket_state") to "SP",
                stringPreferencesKey("supermarket_city") to "Campinas"
            )
        )

        assertEquals(UserLocation("SP", "Campinas"), repository.observeLocation().first())
    }

    @Test
    fun `indexes are cached per state regardless of the city`() = runTest {
        val first = repository.getChainIndexes(UserLocation("SP", "Campinas"))
        val second = repository.getChainIndexes(UserLocation("sp", "Santos"))

        assertTrue(first.isNotEmpty())
        assertSame(first, second)
    }

    @Test
    fun `guanabara is only offered in Rio de Janeiro`() = runTest {
        val rj = repository.getChainIndexes(UserLocation("rj", "Niterói")).map { it.chain.id }
        val sp = repository.getChainIndexes(UserLocation("SP", "Santos")).map { it.chain.id }

        assertTrue("guanabara" in rj)
        assertTrue("guanabara" !in sp)
    }

    @Test
    fun `unknown state has no chains`() = runTest {
        assertTrue(repository.getChainIndexes(UserLocation("XX", "Nowhere")).isEmpty())
    }

    @Test
    fun `saveLocation stores the normalized state and the trimmed city`() = runTest {
        val transform = slot<suspend (Preferences) -> Preferences>()
        var stored: Preferences = emptyPreferences()
        coEvery { dataStore.updateData(capture(transform)) } coAnswers {
            stored = transform.captured(stored)
            stored
        }

        repository.saveLocation(UserLocation(" rj ", " Niterói "))

        assertEquals("RJ", stored[stringPreferencesKey("supermarket_state")])
        assertEquals("Niterói", stored[stringPreferencesKey("supermarket_city")])
    }
}
