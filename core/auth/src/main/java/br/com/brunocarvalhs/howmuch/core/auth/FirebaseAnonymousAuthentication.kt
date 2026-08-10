package br.com.brunocarvalhs.howmuch.core.auth

import br.com.brunocarvalhs.howmuch.core.domain.entity.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.service.AuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class FirebaseAnonymousAuthentication @Inject constructor(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics
) : AuthService {

    override val currentUser: AuthenticatedUser?
        get() = auth.currentUser?.toAuthenticatedUser()

    override suspend fun getOrCreateUserId(): AuthenticatedUser {
        auth.currentUser?.let { user ->
            Timber.d("Sessão já existente: ${user.uid}")
            crashlytics.setUser(user)
            return currentUser ?: error("Usuário nulo após detecção de sessão")
        }

        return try {
            val result = auth.signInAnonymously().await()
            val user = requireNotNull(result.user) { "Usuário nulo após signInAnonymously" }
            Timber.tag(TAG).d("Novo usuário anônimo criado: ${user.uid}")
            crashlytics.setUser(user)
            user.toAuthenticatedUser()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.tag(TAG).e(e, "Falha ao autenticar ou cancelado")
            throw e
        }
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        id = uid,
        email = email,
        displayName = displayName,
        phoneNumber = phoneNumber
    )

    private fun FirebaseCrashlytics.setUser(user: FirebaseUser?) {
        user?.let {
            setUserId(it.uid)
            setCustomKey("user_id", it.uid)
        }
    }

    companion object {
        private const val TAG = "FirebaseAnonymousAuthentication"
    }
}
