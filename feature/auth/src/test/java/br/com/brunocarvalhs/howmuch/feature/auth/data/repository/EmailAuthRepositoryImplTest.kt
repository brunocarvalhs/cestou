package br.com.brunocarvalhs.howmuch.feature.auth.data.repository

import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec EPA-02..EPA-04: every Firebase failure becomes a typed error the UI can place on a field. */
@Suppress("TooManyFunctions")
class EmailAuthRepositoryImplTest {

    private val user = mockk<FirebaseUser>(relaxed = true) {
        every { sendEmailVerification() } returns Tasks.forResult(null)
    }
    private val authResult = mockk<AuthResult> { every { user } returns this@EmailAuthRepositoryImplTest.user }
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val repository = EmailAuthRepositoryImpl(auth)

    private fun Result<Unit>.error(): EmailAuthError =
        (exceptionOrNull() as EmailAuthException).error

    private fun authError(exception: Exception) {
        every { auth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)
        every { auth.sendPasswordResetEmail(any()) } returns Tasks.forException(exception)
    }

    // region sign in

    @Test
    fun `signIn succeeds with valid credentials`() = runTest {
        every { auth.signInWithEmailAndPassword("ana@test.com", "12345678") } returns Tasks.forResult(authResult)

        assertTrue(repository.signIn("ana@test.com", "12345678").isSuccess)
    }

    @Test
    fun `signIn trims the e-mail`() = runTest {
        every { auth.signInWithEmailAndPassword("ana@test.com", "12345678") } returns Tasks.forResult(authResult)

        assertTrue(repository.signIn("  ana@test.com ", "12345678").isSuccess)
    }

    @Test
    fun `wrong password and unknown account map to the same generic error`() = runTest {
        authError(mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true) {
            every { errorCode } returns "ERROR_INVALID_CREDENTIAL"
        })
        assertEquals(EmailAuthError.INVALID_CREDENTIALS, repository.signIn("a@b.co", "x").error())

        authError(mockk<FirebaseAuthInvalidUserException>(relaxed = true) {
            every { errorCode } returns "ERROR_USER_NOT_FOUND"
        })
        assertEquals(EmailAuthError.INVALID_CREDENTIALS, repository.signIn("a@b.co", "x").error())
    }

    @Test
    fun `disabled account maps to its own error`() = runTest {
        authError(mockk<FirebaseAuthInvalidUserException>(relaxed = true) {
            every { errorCode } returns "ERROR_USER_DISABLED"
        })

        assertEquals(EmailAuthError.USER_DISABLED, repository.signIn("a@b.co", "x").error())
    }

    @Test
    fun `too many attempts and network failures map to their own errors`() = runTest {
        authError(mockk<FirebaseTooManyRequestsException>(relaxed = true))
        assertEquals(EmailAuthError.TOO_MANY_REQUESTS, repository.signIn("a@b.co", "x").error())

        authError(mockk<FirebaseNetworkException>(relaxed = true))
        assertEquals(EmailAuthError.NETWORK, repository.signIn("a@b.co", "x").error())
    }

    @Test
    fun `unexpected failures map to unknown`() = runTest {
        authError(IllegalStateException("boom"))

        assertEquals(EmailAuthError.UNKNOWN, repository.signIn("a@b.co", "x").error())
    }

    // endregion

    // region sign up

    @Test
    fun `signUp creates the account and sends a non-blocking verification e-mail`() = runTest {
        every { auth.createUserWithEmailAndPassword("ana@test.com", "12345678") } returns Tasks.forResult(authResult)

        val result = repository.signUp("ana@test.com", "12345678")

        assertTrue(result.isSuccess)
        verify { auth.useAppLanguage() }
        verify { user.sendEmailVerification() }
    }

    @Test
    fun `signUp still succeeds when the verification e-mail fails`() = runTest {
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
        every { user.sendEmailVerification() } returns Tasks.forException(IllegalStateException("quota"))

        assertTrue(repository.signUp("ana@test.com", "12345678").isSuccess)
    }

    @Test
    fun `e-mail already in use maps to its own error`() = runTest {
        authError(mockk<FirebaseAuthUserCollisionException>(relaxed = true))

        assertEquals(EmailAuthError.EMAIL_ALREADY_IN_USE, repository.signUp("a@b.co", "12345678").error())
    }

    @Test
    fun `weak password is not mistaken for invalid credentials`() = runTest {
        // FirebaseAuthWeakPasswordException extends FirebaseAuthInvalidCredentialsException.
        authError(mockk<FirebaseAuthWeakPasswordException>(relaxed = true))

        assertEquals(EmailAuthError.WEAK_PASSWORD, repository.signUp("a@b.co", "123").error())
    }

    @Test
    fun `malformed e-mail maps to invalid e-mail`() = runTest {
        authError(mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true) {
            every { errorCode } returns "ERROR_INVALID_EMAIL"
        })

        assertEquals(EmailAuthError.INVALID_EMAIL, repository.signUp("a@b", "12345678").error())
    }

    // endregion

    // region password reset

    @Test
    fun `sendPasswordReset succeeds and uses the app language`() = runTest {
        every { auth.sendPasswordResetEmail("ana@test.com") } returns Tasks.forResult(null)

        assertTrue(repository.sendPasswordReset(" ana@test.com").isSuccess)
        verify { auth.useAppLanguage() }
    }

    @Test
    fun `sendPasswordReset reports success for an unknown account to prevent enumeration`() = runTest {
        authError(mockk<FirebaseAuthInvalidUserException>(relaxed = true) {
            every { errorCode } returns "ERROR_USER_NOT_FOUND"
        })

        assertTrue(repository.sendPasswordReset("nobody@test.com").isSuccess)
    }

    @Test
    fun `sendPasswordReset still reports network failures`() = runTest {
        authError(mockk<FirebaseNetworkException>(relaxed = true))

        assertEquals(EmailAuthError.NETWORK, repository.sendPasswordReset("a@b.co").error())
    }

    // endregion
}
