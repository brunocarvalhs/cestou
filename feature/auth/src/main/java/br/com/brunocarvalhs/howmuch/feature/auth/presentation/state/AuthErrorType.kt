package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

import com.firebase.ui.auth.AuthException
import com.google.firebase.FirebaseException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * User-facing classification of a sign-in failure, used to pick which copy/icon
 * [br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.AuthErrorBottomSheet]
 * shows on [br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.WelcomeScreen].
 *
 * Firebase UI's own `AuthException.from` (see `firebase-ui-auth:10.0.0-beta03`) already maps a
 * generic [FirebaseException] to [AuthException.NetworkException] — but it does **not** catch
 * every real-world connectivity failure. In particular, Google Sign-In goes through Android's
 * Credential Manager (`androidx.credentials`) before Firebase is ever involved: when Play
 * services can't reach the network, `CredentialProviderBeginSignInController` maps the
 * `ApiException`'s `CommonStatusCodes.NETWORK_ERROR` status to a plain
 * `GetCredentialInterruptedException("NETWORK_ERROR")` (not a [FirebaseException]), which
 * `AuthException.from` then falls through to [AuthException.UnknownException] with a generic
 * "No Google accounts available" message — exactly the confusing, unrelated-sounding message a
 * real device produced for what was actually a Wi-Fi outage. [from] fixes that by walking the
 * exception's cause chain for network signals *before* trusting whatever bucket Firebase UI
 * already put the exception in.
 */
internal enum class AuthErrorType {
    /** No connectivity at all: DNS/host resolution failed, or Play services reported NETWORK_ERROR. */
    NO_CONNECTIVITY,

    /** Reached a network but the request failed/timed out — Firebase Auth's own network bucket. */
    CONNECTION_FAILURE,

    /** Anything else: unexpected/unrecognized failure. Always the safe fallback, never throws. */
    STRUCTURAL;

    companion object {
        private val NO_CONNECTIVITY_SIGNAL = Regex(
            "network_error|connection_suspended_during_call",
            RegexOption.IGNORE_CASE
        )

        fun from(exception: Throwable): AuthErrorType {
            val causeChain = generateSequence(exception) { it.cause }.toList()

            val hasNoConnectivitySignal = causeChain.any { cause ->
                cause is UnknownHostException ||
                    cause.message?.let { NO_CONNECTIVITY_SIGNAL.containsMatchIn(it) } == true
            }
            if (hasNoConnectivitySignal) return NO_CONNECTIVITY

            val hasConnectionFailureSignal = causeChain.any { cause ->
                cause is SocketTimeoutException ||
                    cause is AuthException.NetworkException ||
                    cause is FirebaseException
            }
            if (hasConnectionFailureSignal) return CONNECTION_FAILURE

            return STRUCTURAL
        }
    }
}
