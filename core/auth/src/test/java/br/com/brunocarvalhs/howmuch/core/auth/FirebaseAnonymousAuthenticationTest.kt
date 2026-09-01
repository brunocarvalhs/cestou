package br.com.brunocarvalhs.howmuch.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseAnonymousAuthenticationTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val dataStore = mockk<DataStore<Preferences>>(relaxed = true) {
        every { data } returns flowOf(emptyPreferences())
    }

    private fun fakeUser(uid: String = "user-1") = mockk<FirebaseUser> {
        every { this@mockk.uid } returns uid
        every { email } returns "user@test.com"
        every { displayName } returns "User"
        every { phoneNumber } returns null
        every { photoUrl } returns null
    }

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { auth.currentUser } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `currentUser maps the FirebaseUser to an AuthenticatedUser`() {
        every { auth.currentUser } returns fakeUser("user-1")

        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        assertEquals("user-1", service.currentUser?.id)
        assertEquals("user@test.com", service.currentUser?.email)
    }

    @Test
    fun `currentUser is null when there is no FirebaseUser`() {
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        assertNull(service.currentUser)
    }

    @Test
    fun `getOrCreateUserId returns the existing user without signing in again`() = runTest {
        every { auth.currentUser } returns fakeUser("user-1")
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.getOrCreateUserId()

        assertEquals("user-1", result.id)
        verify(exactly = 0) { auth.signInAnonymously() }
    }

    @Test
    fun `getOrCreateUserId signs in anonymously when there is no current user`() = runTest {
        val authResult = mockk<AuthResult> { every { user } returns fakeUser("user-2") }
        every { auth.signInAnonymously() } returns Tasks.forResult(authResult)
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.getOrCreateUserId()

        assertEquals("user-2", result.id)
    }

    @Test
    fun `signInAnonymously returns success with the mapped user`() = runTest {
        val authResult = mockk<AuthResult> { every { user } returns fakeUser("user-2") }
        every { auth.signInAnonymously() } returns Tasks.forResult(authResult)
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.signInAnonymously()

        assertTrue(result.isSuccess)
        assertEquals("user-2", result.getOrNull()?.id)
    }

    @Test
    fun `signInAnonymously fails when Firebase returns a null user`() = runTest {
        val authResult = mockk<AuthResult> { every { user } returns null }
        every { auth.signInAnonymously() } returns Tasks.forResult(authResult)
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.signInAnonymously()

        assertTrue(result.isFailure)
    }

    @Test
    fun `signInAnonymously fails when the Firebase task fails`() = runTest {
        every { auth.signInAnonymously() } returns Tasks.forException(RuntimeException("no network"))
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.signInAnonymously()

        assertTrue(result.isFailure)
    }

    @Test
    fun `signOut succeeds and delegates to FirebaseAuth`() = runTest {
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.signOut()

        assertTrue(result.isSuccess)
        verify { auth.signOut() }
    }

    @Test
    fun `signOut fails when FirebaseAuth throws`() = runTest {
        every { auth.signOut() } throws IllegalStateException("boom")
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.signOut()

        assertTrue(result.isFailure)
    }

    @Test
    fun `signInWithGoogle and signInWithApple are not implemented yet`() = runTest {
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        assertTrue(service.signInWithGoogle().isFailure)
        assertTrue(service.signInWithApple().isFailure)
    }

    @Test
    fun `deleteAccount succeeds and clears the synced user id`() = runTest {
        val user = fakeUser("user-1")
        every { auth.currentUser } returns user
        every { user.delete() } returns Tasks.forResult(null)
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.deleteAccount()

        assertTrue(result.isSuccess)
        verify { user.delete() }
    }

    @Test
    fun `deleteAccount fails when there is no current user`() = runTest {
        every { auth.currentUser } returns null
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.deleteAccount()

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteAccount fails when Firebase throws`() = runTest {
        val user = fakeUser("user-1")
        every { auth.currentUser } returns user
        every { user.delete() } returns Tasks.forException(RuntimeException("recent login required"))
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        val result = service.deleteAccount()

        assertTrue(result.isFailure)
    }

    @Test
    fun `authState reflects the listener registered on FirebaseAuth`() {
        val listenerSlot = slot<FirebaseAuth.AuthStateListener>()
        every { auth.addAuthStateListener(capture(listenerSlot)) } returns Unit
        val service = FirebaseAnonymousAuthentication(auth, crashlytics, dataStore)

        every { auth.currentUser } returns fakeUser("user-3")
        listenerSlot.captured.onAuthStateChanged(auth)

        assertEquals("user-3", service.currentUser?.id)
    }
}
