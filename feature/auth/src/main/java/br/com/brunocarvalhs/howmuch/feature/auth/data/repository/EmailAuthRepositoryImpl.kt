package br.com.brunocarvalhs.howmuch.feature.auth.data.repository

import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Talks to FirebaseAuth directly instead of FirebaseUI's email flow: FirebaseUI blocks every
 * unverified password user on a verification screen (it would strand the Play reviewer) and only
 * reports errors as display strings, which cannot be placed on a field.
 */
internal class EmailAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
) : EmailAuthRepository {

    override suspend fun signIn(email: String, password: String): Result<Unit> = call {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = call {
        auth.useAppLanguage()
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
        try {
            user?.sendEmailVerification()?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Verification never blocks the account (spec decision 3); losing the e-mail is fine.
            Timber.w(e, "Falha ao enviar e-mail de verificação")
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = call {
        auth.useAppLanguage()
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // Same confirmation as success: the screen must not reveal which e-mails have accounts.
            Timber.d("Recuperação de senha para conta inexistente (%s)", e.errorCode)
        }
    }

    private suspend fun call(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(EmailAuthException(e.toEmailAuthError(), e))
    }

    private fun Exception.toEmailAuthError(): EmailAuthError = when (this) {
        // Before InvalidCredentials: the weak-password exception is a subclass of it.
        is FirebaseAuthWeakPasswordException -> EmailAuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException ->
            if (errorCode == "ERROR_INVALID_EMAIL") EmailAuthError.INVALID_EMAIL else EmailAuthError.INVALID_CREDENTIALS
        is FirebaseAuthInvalidUserException ->
            if (errorCode == "ERROR_USER_DISABLED") EmailAuthError.USER_DISABLED else EmailAuthError.INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException -> EmailAuthError.EMAIL_ALREADY_IN_USE
        is FirebaseTooManyRequestsException -> EmailAuthError.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> EmailAuthError.NETWORK
        else -> EmailAuthError.UNKNOWN
    }
}
