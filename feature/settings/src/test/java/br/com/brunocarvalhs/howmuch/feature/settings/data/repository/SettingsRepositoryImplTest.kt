package br.com.brunocarvalhs.howmuch.feature.settings.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsRepositoryImplTest {

    private val dataStore = mockk<DataStore<Preferences>>()
    private val context = mockk<Context>(relaxed = true)
    private val repository = SettingsRepositoryImpl(dataStore, context)

    @Test
    fun `getSettings falls back to defaults when no preferences are stored`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repository.getSettings().test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals("gemini", settings.aiProvider)
            assertEquals("CATEGORY", settings.sortingMode)
            assertFalse(settings.remindersEnabled)
            assertEquals("pt", settings.language)
            assertEquals("BRL", settings.currency)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings reads stored values`() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("theme_mode") to ThemeMode.DARK.name,
            stringPreferencesKey("ai_provider") to "openrouter",
            booleanPreferencesKey("reminders_enabled") to true,
            stringPreferencesKey("language") to "en",
            stringPreferencesKey("currency") to "USD"
        )
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            val settings = awaitItem()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertEquals("openrouter", settings.aiProvider)
            assertTrue(settings.remindersEnabled)
            assertEquals("en", settings.language)
            assertEquals("USD", settings.currency)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings falls back to SYSTEM theme when stored value is invalid`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "NOT_A_THEME")
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
            awaitComplete()
        }
    }

    /**
     * `DataStore<Preferences>.edit { }` is a real extension that wraps the caller's transform
     * and delegates to the interface's `updateData`, so the mock has to intercept `updateData`
     * (not `edit`) and thread the resulting [Preferences] through calls to observe what each
     * repository method actually wrote.
     */
    private class FakeDataStore(initial: Preferences) {
        var current: Preferences = initial
        operator fun <T> get(key: Preferences.Key<T>): T? = current[key]
        fun contains(key: Preferences.Key<*>): Boolean = current.asMap().containsKey(key)
        fun isEmpty(): Boolean = current.asMap().isEmpty()
    }

    private fun mockEdit(initial: Preferences = emptyPreferences()): FakeDataStore {
        val fake = FakeDataStore(initial)
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } coAnswers {
            fake.current = transformSlot.captured(fake.current)
            fake.current
        }
        return fake
    }

    @Test
    fun `updateTheme stores the new theme mode`() = runTest {
        val fake = mockEdit()

        repository.updateTheme(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK.name, fake[stringPreferencesKey("theme_mode")])
    }

    @Test
    fun `updateNotificationSettings stores enabled flag and reminder time`() = runTest {
        val fake = mockEdit()

        repository.updateNotificationSettings(enabled = true, reminderTime = "08:00")

        assertEquals(true, fake[booleanPreferencesKey("notifications_enabled")])
        assertEquals("08:00", fake[stringPreferencesKey("reminder_time")])
    }

    @Test
    fun `updateAiSettings stores model, gemini provider and prompt`() = runTest {
        val fake = mockEdit()

        repository.updateAiSettings(model = "google/gemini-2.0-flash-001", prompt = "be concise", creativity = 0.9f)

        assertEquals("google/gemini-2.0-flash-001", fake[stringPreferencesKey("ai_model")])
        assertEquals("gemini", fake[stringPreferencesKey("ai_provider")])
        assertEquals("be concise", fake[stringPreferencesKey("custom_prompt")])
    }

    @Test
    fun `updateAiSettings removes prompt when null`() = runTest {
        val fake = mockEdit(initial = mutablePreferencesOf(stringPreferencesKey("custom_prompt") to "old prompt"))

        repository.updateAiSettings(model = "some/model", prompt = null, creativity = 0.5f)

        assertFalse(fake.contains(stringPreferencesKey("custom_prompt")))
        assertEquals("openrouter", fake[stringPreferencesKey("ai_provider")])
    }

    @Test
    fun `updateShoppingPreferences stores default list and sorting mode`() = runTest {
        val fake = mockEdit()

        repository.updateShoppingPreferences(defaultListId = "list-1", sortingMode = "NAME", remindersEnabled = true)

        assertEquals("list-1", fake[stringPreferencesKey("default_list_id")])
        assertEquals("NAME", fake[stringPreferencesKey("sorting_mode")])
        assertEquals(true, fake[booleanPreferencesKey("reminders_enabled")])
    }

    @Test
    fun `updateShoppingPreferences removes default list when null`() = runTest {
        val fake = mockEdit(initial = mutablePreferencesOf(stringPreferencesKey("default_list_id") to "list-1"))

        repository.updateShoppingPreferences(defaultListId = null, sortingMode = "CATEGORY", remindersEnabled = false)

        assertFalse(fake.contains(stringPreferencesKey("default_list_id")))
    }

    @Test
    fun `updateLanguage stores the language`() = runTest {
        val fake = mockEdit()

        repository.updateLanguage("es")

        assertEquals("es", fake[stringPreferencesKey("language")])
    }

    @Test
    fun `updateCurrency stores the currency`() = runTest {
        val fake = mockEdit()

        repository.updateCurrency("EUR")

        assertEquals("EUR", fake[stringPreferencesKey("currency")])
    }

    @Test
    fun `clearCache deletes the cache directory`() = runTest {
        val cacheDir = mockk<File>(relaxed = true)
        every { context.cacheDir } returns cacheDir

        repository.clearCache()

        verify { cacheDir.deleteRecursively() }
    }

    @Test
    fun `deleteAllData clears preferences and cache`() = runTest {
        val fake = mockEdit(initial = mutablePreferencesOf(stringPreferencesKey("language") to "en"))
        val cacheDir = mockk<File>(relaxed = true)
        every { context.cacheDir } returns cacheDir

        repository.deleteAllData()

        assertTrue(fake.isEmpty())
        verify { cacheDir.deleteRecursively() }
    }
}
