package br.com.brunocarvalhs.howmuch.core.auth

import br.com.brunocarvalhs.howmuch.core.auth.di.AuthDataStore
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.get
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class FirebaseAnonymousAuthentication @Inject constructor(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
    @AuthDataStore private val storage: StorageService
) : AuthService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _firebaseAuthState = MutableStateFlow(auth.currentUser?.toAuthenticatedUser())
    private val _syncedUserId = MutableStateFlow<String?>(null)

    override val authState: Flow<AuthenticatedUser?> = combine(
        _firebaseAuthState,
        _syncedUserId
    ) { firebaseUser, syncedId ->
        if (syncedId != null) {
            firebaseUser?.copy(id = syncedId) ?: AuthenticatedUser(id = syncedId)
        } else {
            firebaseUser
        }
    }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _firebaseAuthState.value = firebaseAuth.currentUser?.toAuthenticatedUser()
        }
        scope.launch {
            storage.observe<String>(USER_ID_KEY).collect {
                _syncedUserId.value = it
            }
        }
    }

    override val currentUser: AuthenticatedUser?
        get() {
            val syncedId = _syncedUserId.value
            val firebaseUser = _firebaseAuthState.value
            return if (syncedId != null) {
                firebaseUser?.copy(id = syncedId) ?: AuthenticatedUser(id = syncedId)
            } else {
                firebaseUser
            }
        }

    override suspend fun getOrCreateUserId(): AuthenticatedUser {
        val syncedId = storage.get<String>(USER_ID_KEY)
        if (syncedId != null) {
            return AuthenticatedUser(id = syncedId)
        }

        auth.currentUser?.let { user ->
            Timber.d("Sessão já existente: ${user.uid}")
            crashlytics.setUser(user)
            return user.toAuthenticatedUser()
        }

        return signInAnonymously().getOrElse {
            Timber.tag(TAG).w("Fallback para usuário guest devido a falha na autenticação")
            AuthenticatedUser(id = GUEST_ID)
        }
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
        storage.remove(USER_ID_KEY)
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

    override suspend fun updateUserId(userId: String) {
        Timber.tag(TAG).d("Updating user ID to: $userId (Linking account)")
        scope.launch {
            storage.save(USER_ID_KEY, userId)
        }
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
        private const val GUEST_ID = "guest"
        private const val USER_ID_KEY = "synced_user_id"
    }
}
