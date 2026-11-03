package br.com.brunocarvalhs.howmuch.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.SaveResult
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Split from [NotificationRepositoryImplTest]: `FirebaseFirestoreException.Code` cannot be
 * initialized on a plain JVM, so the cases that build one run under Robolectric.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NotificationRepositoryImplSaveTest {

    private val networkService = mockk<NetworkService>()
    private val repository = NotificationRepositoryImpl(networkService)

    private val campaign = Notification(
        id = "fcm-1",
        userId = "u1",
        title = "t",
        message = "m",
        type = "campaign",
        isRead = false,
        timestamp = 5L
    )

    private fun failingWith(code: FirebaseFirestoreException.Code) {
        coEvery { networkService.make<String>(any(), any(), any()) } throws NetworkService.NetworkException(
            message = code.name,
            cause = FirebaseFirestoreException(code.name, code)
        )
    }

    @Test
    fun `save reports an existing document as already saved`() = runTest {
        failingWith(FirebaseFirestoreException.Code.PERMISSION_DENIED)

        assertEquals(SaveResult.AlreadySaved, repository.save(campaign).getOrNull())
    }

    @Test
    fun `save keeps any other failure as a failure`() = runTest {
        failingWith(FirebaseFirestoreException.Code.UNAVAILABLE)

        assertTrue(repository.save(campaign).isFailure)
    }
}
