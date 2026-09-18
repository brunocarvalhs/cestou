package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

import com.firebase.ui.auth.AuthException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthErrorTypeTest {

    @Test
    fun `classifies UnknownHostException as no connectivity`() {
        val exception = UnknownHostException("Unable to resolve host")

        assertEquals(AuthErrorType.NO_CONNECTIVITY, AuthErrorType.from(exception))
    }

    @Test
    fun `classifies a GMS NETWORK_ERROR wrapped by AuthUnknownException as no connectivity`() {
        // Reproduces the real device bug: Credential Manager wraps a GMS
        // CommonStatusCodes#NETWORK_ERROR as a plain (non-Firebase) exception, which Firebase
        // UI's own AuthException.from() then falls back to UnknownException for — losing the
        // network signal unless we look at the cause chain ourselves.
        val gmsNetworkError = IllegalStateException("NETWORK_ERROR")
        val wrapped = AuthException.UnknownException(
            message = "No Google accounts available.",
            cause = gmsNetworkError
        )

        assertEquals(AuthErrorType.NO_CONNECTIVITY, AuthErrorType.from(wrapped))
    }

    @Test
    fun `classifies AuthException NetworkException as connection failure`() {
        // Firebase Auth's own SDK collapses timeouts, interrupted connections, and unreachable
        // hosts into a single FirebaseNetworkException, which Firebase UI maps to
        // AuthException.NetworkException — so this is intentionally the generic "connection"
        // bucket rather than trying to fake a finer distinction the SDK doesn't provide.
        val exception = AuthException.NetworkException(message = "A network error occurred")

        assertEquals(AuthErrorType.CONNECTION_FAILURE, AuthErrorType.from(exception))
    }

    @Test
    fun `classifies SocketTimeoutException as connection failure`() {
        val exception = SocketTimeoutException("timeout")

        assertEquals(AuthErrorType.CONNECTION_FAILURE, AuthErrorType.from(exception))
    }

    @Test
    fun `classifies an unrecognized exception as structural`() {
        val exception = IllegalStateException("something exploded")

        assertEquals(AuthErrorType.STRUCTURAL, AuthErrorType.from(exception))
    }

    @Test
    fun `classifies AuthException UnknownException without a network cause as structural`() {
        val exception = AuthException.UnknownException(message = "Unexpected error")

        assertEquals(AuthErrorType.STRUCTURAL, AuthErrorType.from(exception))
    }

    @Test
    fun `never throws for an exception with a self-referential cause`() {
        val exception = IllegalStateException("weird")

        // Should terminate instead of looping forever if cause chains are ever cyclic.
        AuthErrorType.from(exception)
    }
}
