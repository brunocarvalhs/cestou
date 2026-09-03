package br.com.brunocarvalhs.howmuch.core.data.network

import br.com.brunocarvalhs.howmuch.core.data.security.CryptoManager
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@Serializable
private data class TestModel(val id: String, val title: String)

class NetworkManagerTest {

    private lateinit var firebaseFirestoreManager: FirebaseFirestoreManager
    private lateinit var networkLogger: NetworkLogger
    private lateinit var manager: NetworkManager

    private val request = NetworkService.NetworkRequest(
        endpoint = "shopping",
        method = NetworkService.Method.GET
    )

    @Before
    fun setup() {
        firebaseFirestoreManager = mockk()
        networkLogger = mockk(relaxed = true)
        manager = NetworkManager(
            firebaseFirestoreManager = firebaseFirestoreManager,
            cryptoManager = CryptoManager(),
            compatibilityConverter = CompatibilityConverter(),
            networkLogger = networkLogger
        )
    }

    @Test
    fun `make decodes a single object response`() = runTest {
        coEvery {
            firebaseFirestoreManager.execute(any(), any(), any(), any())
        } returns mapOf("id" to "1", "title" to "Weekly Groceries")

        val result = manager.make(request, TestModel::class, null)

        assertEquals(TestModel("1", "Weekly Groceries"), result)
    }

    @Test
    fun `make decodes a list of objects response`() = runTest {
        coEvery {
            firebaseFirestoreManager.execute(any(), any(), any(), any())
        } returns listOf(
            mapOf("id" to "1", "title" to "A"),
            mapOf("id" to "2", "title" to "B")
        )

        val result = manager.make(
            request,
            List::class,
            kotlin.reflect.typeOf<List<TestModel>>()
        )

        assertEquals(listOf(TestModel("1", "A"), TestModel("2", "B")), result)
    }

    @Test
    fun `make returns null when the raw response is null`() = runTest {
        coEvery { firebaseFirestoreManager.execute(any(), any(), any(), any()) } returns null

        val result = manager.make(request, TestModel::class, null)

        assertNull(result)
    }

    @Test
    fun `make returns null and logs failure when execute throws`() = runTest {
        coEvery {
            firebaseFirestoreManager.execute(any(), any(), any(), any())
        } throws IllegalStateException("boom")

        val result = manager.make(request, TestModel::class, null)

        assertNull(result)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `make rethrows CancellationException instead of swallowing it`() = runTest {
        coEvery {
            firebaseFirestoreManager.execute(any(), any(), any(), any())
        } throws kotlinx.coroutines.CancellationException("cancelled")

        manager.make(request, TestModel::class, null)
    }

    @Test
    fun `make returns null when the response shape can't be decoded and there is no fallback`() = runTest {
        coEvery {
            firebaseFirestoreManager.execute(any(), any(), any(), any())
        } returns mapOf("unexpected" to "shape")

        val result = manager.make(request, TestModel::class, null)

        assertNull(result)
    }

    @Test
    fun `make falls back to a typed array when the target type is an array`() = runTest {
        coEvery {
            firebaseFirestoreManager.execute(any(), any(), any(), any())
        } returns listOf("plain text value")

        val result = manager.make(request, Array<String>::class, null)

        assertEquals(listOf("plain text value"), (result as Array<*>).toList())
    }

    @Test
    fun `make decrypts encrypted map values before decoding`() = runTest {
        val crypto = CryptoManager()
        coEvery {
            firebaseFirestoreManager.execute(any(), any(), any(), any())
        } returns mapOf("id" to crypto.encrypt("\"1\""), "title" to crypto.encrypt("\"Encrypted\""))

        val result = manager.make(request, TestModel::class, null)

        assertEquals(TestModel("1", "Encrypted"), result)
    }

    @Test
    fun `observe maps each emission through response decoding`() = runTest {
        every {
            firebaseFirestoreManager.observe(any(), any())
        } returns flowOf(mapOf("id" to "1", "title" to "A"), mapOf("id" to "2", "title" to "B"), null)

        val results = manager.observe(request, TestModel::class, null).toList()

        assertEquals(
            listOf(TestModel("1", "A"), TestModel("2", "B"), null),
            results
        )
    }
}
