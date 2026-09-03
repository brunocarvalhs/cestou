package br.com.brunocarvalhs.howmuch.core.data.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.brunocarvalhs.howmuch.core.domain.services.get
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@Serializable
private data class TestModel(val id: String, val name: String)

class DataStoreStorageServiceTest {

    private val dataStore = mockk<DataStore<Preferences>>()

    private class FakeDataStore(initial: Preferences) {
        var current: Preferences = initial
    }

    private fun mockEdit(initial: Preferences = emptyPreferences()): FakeDataStore {
        val fake = FakeDataStore(initial)
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } coAnswers {
            fake.current = transformSlot.captured(fake.current)
            fake.current
        }
        every { dataStore.data } answers { flowOf(fake.current) }
        return fake
    }

    @Test
    fun `save and get round-trip a String value raw`() = runTest {
        val fake = mockEdit()
        val service = DataStoreStorageService(dataStore)

        service.save("token", "abc-123")

        assertEquals("abc-123", fake.current[stringPreferencesKey("token")])
        assertEquals("abc-123", service.get<String>("token"))
    }

    @Test
    fun `save and get round-trip a serializable object as JSON`() = runTest {
        mockEdit()
        val service = DataStoreStorageService(dataStore)

        service.save("model", TestModel(id = "1", name = "Cestou"))

        assertEquals(TestModel(id = "1", name = "Cestou"), service.get<TestModel>("model"))
    }

    @Test
    fun `get returns null when the key is missing`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())
        val service = DataStoreStorageService(dataStore)

        assertNull(service.get<String>("missing"))
    }

    @Test
    fun `get returns null when the stored value cannot be decoded`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("model") to "not-json")
        every { dataStore.data } returns flowOf(stored)
        val service = DataStoreStorageService(dataStore)

        assertNull(service.get<TestModel>("model"))
    }

    @Test
    fun `remove deletes the key`() = runTest {
        val fake = mockEdit(initial = mutablePreferencesOf(stringPreferencesKey("token") to "abc"))
        val service = DataStoreStorageService(dataStore)

        service.remove("token")

        assertTrue(fake.current.asMap().isEmpty())
    }

    @Test
    fun `clear removes every key`() = runTest {
        val fake = mockEdit(
            initial = mutablePreferencesOf(
                stringPreferencesKey("token") to "abc",
                stringPreferencesKey("model") to "{}"
            )
        )
        val service = DataStoreStorageService(dataStore)

        service.clear()

        assertTrue(fake.current.asMap().isEmpty())
    }

    @Test
    fun `observe emits decoded updates and null when absent`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("token") to "abc-123")
        every { dataStore.data } returns flowOf(emptyPreferences(), stored)
        val service = DataStoreStorageService(dataStore)

        val values = service.observe<String>("token").toList()

        assertEquals(listOf(null, "abc-123"), values)
    }

    @Test
    fun `observe falls back to empty preferences when the DataStore throws IOException`() = runTest {
        every { dataStore.data } returns kotlinx.coroutines.flow.flow { throw IOException("disk error") }
        val service = DataStoreStorageService(dataStore)

        val values = service.observe<String>("token").toList()

        assertEquals(listOf(null), values)
    }
}
