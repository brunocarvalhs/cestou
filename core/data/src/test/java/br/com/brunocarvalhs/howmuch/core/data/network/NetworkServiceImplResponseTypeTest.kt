package br.com.brunocarvalhs.howmuch.core.data.network

import br.com.brunocarvalhs.howmuch.core.data.security.Encryptor
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for the Crashlytics non-fatal (356 events/297 users, 2.1.1-2.2.0):
 * `FirestoreClient.post()` always returns the document id (e.g. a Firebase UID) as a raw String.
 * Split out from [NetworkServiceImplTest] to keep each test class focused on one concern.
 */
class NetworkServiceImplResponseTypeTest {

    private lateinit var firestoreClient: FirestoreClient
    private lateinit var networkLogger: NetworkLogger
    private lateinit var manager: NetworkServiceImpl

    private val request = NetworkService.NetworkRequest(
        endpoint = "shopping",
        method = NetworkService.Method.GET
    )

    @Before
    fun setup() {
        firestoreClient = mockk()
        networkLogger = mockk(relaxed = true)
        manager = NetworkServiceImpl(
            firestoreClient = firestoreClient,
            encryptor = Encryptor(),
            compatibilityConverter = CompatibilityConverter(),
            networkLogger = networkLogger
        )
    }

    @Test
    fun `make decodes a Firestore document id as a String without a serialization error`() = runTest {
        // Decoding a document id against the real String::class response type must succeed and
        // must not go through the SerializationException fallback path at all.
        val firebaseUid = "xYta46iQyMW2gKcOkjWKuhLTBGr1"
        coEvery { firestoreClient.execute(any(), any(), any(), any()) } returns firebaseUid

        val result = manager.make(request, String::class, null)

        assertEquals(firebaseUid, result)
        verify(exactly = 0) { networkLogger.serializationError(any(), any(), any()) }
    }

    @Test
    fun `make logs a serialization error when a Firestore document id is decoded as Boolean`() = runTest {
        // Documents the bug this module used to have before the response type was corrected at
        // the call sites (e.g. UserRepositoryImpl.saveProfile): asking NetworkServiceImpl to
        // decode a document-id string as a Boolean throws a JsonDecodingException, which is
        // caught here and reported as a non-fatal instead of being allowed to crash the app.
        val firebaseUid = "xYta46iQyMW2gKcOkjWKuhLTBGr1"
        coEvery { firestoreClient.execute(any(), any(), any(), any()) } returns firebaseUid

        val result = manager.make(request, Boolean::class, null)

        assertNull(result)
        verify { networkLogger.serializationError(Boolean::class, firebaseUid, any()) }
    }
}
