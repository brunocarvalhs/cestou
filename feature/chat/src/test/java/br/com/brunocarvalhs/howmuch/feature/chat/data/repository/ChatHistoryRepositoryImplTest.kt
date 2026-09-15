package br.com.brunocarvalhs.howmuch.feature.chat.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import br.com.brunocarvalhs.howmuch.core.data.service.DataStoreStorageService
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

private const val FIRST_MESSAGE_EPOCH_MILLIS = 1_000L
private const val SECOND_MESSAGE_EPOCH_MILLIS = 2_000L

// Exercises the real DataStoreStorageService (JSON-backed) instead of a hand-rolled fake:
// a fake that stores/returns the object as-is would never catch a value::class.serializer()
// resolution failure like the ArrayList crash this repository used to hit on every send.
class ChatHistoryRepositoryImplTest {

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

    private fun repository(): ChatHistoryRepositoryImpl =
        ChatHistoryRepositoryImpl(DataStoreStorageService(dataStore))

    @Test
    fun `load returns empty list when nothing was saved yet`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        assertTrue(repository().load("list1").isEmpty())
    }

    @Test
    fun `save then load round-trips the messages for that shopping id`() = runTest {
        mockEdit()
        val repository = repository()
        val messages = listOf(
            ChatMessage(
                id = 1,
                text = "quanto vou gastar?",
                sender = ChatMessage.Sender.USER,
                createdAt = Instant.ofEpochMilli(FIRST_MESSAGE_EPOCH_MILLIS)
            ),
            ChatMessage(
                id = 2,
                text = "R$ 87,40 😊",
                sender = ChatMessage.Sender.ASSISTANT,
                createdAt = Instant.ofEpochMilli(SECOND_MESSAGE_EPOCH_MILLIS)
            )
        )

        repository.save("list1", messages)

        assertEquals(messages, repository.load("list1"))
    }

    @Test
    fun `messages are scoped per shopping id`() = runTest {
        val fake = mockEdit()
        val repository = repository()
        val listOneMessages = listOf(
            ChatMessage(
                id = 1,
                text = "list one",
                sender = ChatMessage.Sender.USER,
                createdAt = Instant.ofEpochMilli(FIRST_MESSAGE_EPOCH_MILLIS)
            )
        )

        repository.save("list1", listOneMessages)
        every { dataStore.data } answers { flowOf(fake.current) }

        assertTrue(repository.load("list2").isEmpty())
        assertEquals(listOneMessages, repository.load("list1"))
    }
}
