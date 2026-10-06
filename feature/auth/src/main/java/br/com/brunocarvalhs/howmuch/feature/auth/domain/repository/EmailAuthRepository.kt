package br.com.brunocarvalhs.howmuch.feature.auth.domain.repository

/** Failures are always an [br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException]. */
internal interface EmailAuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Creates the account (signing it in) and sends a verification e-mail that does not block use. */
    suspend fun signUp(email: String, password: String): Result<Unit>

    /** Succeeds for unknown accounts too, so the screen cannot reveal which e-mails exist. */
    suspend fun sendPasswordReset(email: String): Result<Unit>
}
