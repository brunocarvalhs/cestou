package br.com.brunocarvalhs.howmuch.core.auth

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class FirebaseAnonymousAuthentication @Inject constructor(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics
) : AuthService {

    private val _authState = MutableStateFlow(auth.currentUser?.toAuthenticatedUser())
    override val authState: Flow<AuthenticatedUser?> = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = firebaseAuth.currentUser?.toAuthenticatedUser()
        }
    }

    override val currentUser: AuthenticatedUser?
        get() = auth.currentUser?.toAuthenticatedUser()

    override suspend fun getOrCreateUserId(): AuthenticatedUser {
        auth.currentUser?.let { user ->
            Timber.d("Sessão já existente: ${user.uid}")
            crashlytics.setUser(user)
            return user.toAuthenticatedUser()
        }

        return signInAnonymously().getOrThrow()
    }

    override suspend fun signInAnonymously(): Result<AuthenticatedUser> = try {
        val result = auth.signInAnonymously().await()
        val user = requireNotNull(result.user) { "Usuário nulo após signInAnonymously" }
        Timber.tag(TAG).d("Novo usuário anônimo criado: ${user.uid}")
        crashlytics.setUser(user)
        Result.success(user.toAuthenticatedUser())
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Timber.tag(TAG).e(e, "Falha ao autenticar anônimo")
        Result.failure(e)
    }

    override suspend fun signOut(): Result<Unit> = try {
        auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun signInWithGoogle(): Result<AuthenticatedUser> {
        // TODO: Implementar Google Sign In real
        return Result.failure(NotImplementedError("Google Sign In não implementado"))
    }

    override suspend fun signInWithApple(): Result<AuthenticatedUser> {
        // TODO: Implementar Apple Sign In real
        return Result.failure(NotImplementedError("Apple Sign In não implementado"))
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        id = uid,
        email = email,
        displayName = displayName,
        phoneNumber = phoneNumber,
        photoUrl = photoUrl?.toString()
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
