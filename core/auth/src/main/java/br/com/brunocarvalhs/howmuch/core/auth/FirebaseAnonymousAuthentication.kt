package br.com.brunocarvalhs.howmuch.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.brunocarvalhs.howmuch.core.auth.di.AuthDataStore
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class FirebaseAnonymousAuthentication @Inject constructor(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
    @AuthDataStore private val dataStore: DataStore<Preferences>
) : AuthService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _userIdKey = stringPreferencesKey("synced_user_id")

    private val _firebaseAuthState = MutableStateFlow(auth.currentUser?.toAuthenticatedUser())
    private val _syncedUserId = MutableStateFlow<String?>(null)

    private val resolveUserIdMutex = Mutex()

    // Caches the id resolved by getOrCreateUserId() so every caller (create/read/observe) agrees
    // on the same UUID even when resolved concurrently before any session is persisted.
    @Volatile
    private var cachedUserId: String? = null

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
            dataStore.data.map { it[_userIdKey] }.collect {
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
        cachedUserId?.let { return AuthenticatedUser(id = it) }

        return resolveUserIdMutex.withLock {
            // Re-check: another caller may have resolved it while we were waiting for the lock.
            cachedUserId?.let { return@withLock AuthenticatedUser(id = it) }

            val syncedId = dataStore.data.map { it[_userIdKey] }.firstOrNull()
            if (syncedId != null) {
                cachedUserId = syncedId
                return@withLock AuthenticatedUser(id = syncedId)
            }

            auth.currentUser?.let { user ->
                Timber.d("Sessão já existente: ${user.uid}")
                crashlytics.setUser(user)
                dataStore.edit { it[_userIdKey] = user.uid }
                cachedUserId = user.uid
                return@withLock user.toAuthenticatedUser()
            }

            val result = signInAnonymously().getOrElse {
                Timber.tag(TAG).w("Fallback para usuário guest devido a falha na autenticação")
                AuthenticatedUser(id = GUEST_ID)
            }
            if (result.id != GUEST_ID) {
                dataStore.edit { it[_userIdKey] = result.id }
                cachedUserId = result.id
            }
            result
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
        dataStore.edit { it.remove(_userIdKey) }
        cachedUserId = null
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

    override suspend fun deleteAccount(): Result<Unit> = try {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No authenticated user"))
        user.delete().await()
        dataStore.edit { it.remove(_userIdKey) }
        Result.success(Unit)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Timber.tag(TAG).e(e, "Falha ao excluir conta")
        Result.failure(e)
    }

    override suspend fun updateUserId(userId: String) {
        Timber.tag(TAG).d("Updating user ID to: $userId (Linking account)")
        dataStore.edit { preferences ->
            preferences[_userIdKey] = userId
        }
        cachedUserId = userId
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
    }
}
