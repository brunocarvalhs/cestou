package br.com.brunocarvalhs.howmuch.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.services.ReminderScheduler
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
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)
    private val repository = SettingsRepositoryImpl(dataStore, context, reminderScheduler)

    @Test
    fun `getSettings falls back to defaults when no preferences are stored`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repository.getSettings().test {
            val settings = awaitItem()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals("openrouter", settings.aiProvider)
            assertEquals("CATEGORY", settings.sortingMode)
            assertFalse(settings.remindersEnabled)
            assertEquals("pt-BR", settings.language)
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

    // region sdd-006 (B-07): migrating a legacy vendor-named provider to the technical one

    @Test
    fun `getSettings migrates a legacy vendor-named provider to openrouter for a known model`() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("ai_provider") to "nvidia",
            stringPreferencesKey("ai_model") to "nvidia/nemotron-3-ultra"
        )
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            assertEquals("openrouter", awaitItem().aiProvider)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings migrates an unknown legacy provider to openrouter when the model is also unknown`() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("ai_provider") to "some-old-vendor",
            stringPreferencesKey("ai_model") to "some/unknown-model"
        )
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            assertEquals("openrouter", awaitItem().aiProvider)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings leaves an already-technical gemini provider untouched`() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("ai_provider") to "gemini",
            stringPreferencesKey("ai_model") to "gemini-1.5-pro"
        )
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            assertEquals("gemini", awaitItem().aiProvider)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings migration is idempotent`() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("ai_provider") to "nvidia",
            stringPreferencesKey("ai_model") to "nvidia/nemotron-3-ultra"
        )
        every { dataStore.data } returns flowOf(stored)

        var firstProvider: String? = null
        var secondProvider: String? = null
        repository.getSettings().test {
            firstProvider = awaitItem().aiProvider
            awaitComplete()
        }
        repository.getSettings().test {
            secondProvider = awaitItem().aiProvider
            awaitComplete()
        }

        assertEquals("openrouter", firstProvider)
        assertEquals(firstProvider, secondProvider)
    }

    // endregion

    // region aiModel migration — a stored model id no longer in the catalog resets to the default

    @Test
    fun `getSettings resets a stale ai_model to the default`() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("ai_model") to "openrouter/auto"
        )
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            assertEquals("openrouter/free", awaitItem().aiModel)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings keeps a stored ai_model that is still in the catalog`() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("ai_model") to "cohere/north-mini-code:free"
        )
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            assertEquals("cohere/north-mini-code:free", awaitItem().aiModel)
            awaitComplete()
        }
    }

    // endregion

    @Test
    fun `getSettings falls back to SYSTEM theme when stored value is invalid`() = runTest {
        val stored = mutablePreferencesOf(stringPreferencesKey("theme_mode") to "NOT_A_THEME")
        every { dataStore.data } returns flowOf(stored)

        repository.getSettings().test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
            awaitComplete()
        }
    }

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
        every { dataStore.data } answers { flowOf(fake.current) }
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
    fun `updateAiSettings stores model and provider`() = runTest {
        val fake = mockEdit()

        repository.updateAiSettings(
            tier = AiConfigTier.DEFAULT,
            model = "google/gemini-2.0-flash-001",
            creativity = 0.9f,
            customApiKey = null
        )

        assertEquals("google/gemini-2.0-flash-001", fake[stringPreferencesKey("ai_model")])
        assertEquals("gemini", fake[stringPreferencesKey("ai_provider")])
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
