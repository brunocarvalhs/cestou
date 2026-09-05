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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class FirebaseAnonymousAuthentication @Inject constructor(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
    @AuthDataStore private val storage: StorageService
) : AuthService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _firebaseAuthState = MutableStateFlow(auth.currentUser?.toAuthenticatedUserOrNull())
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
            _firebaseAuthState.value = firebaseAuth.currentUser?.toAuthenticatedUserOrNull()
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
        cachedUserId?.let { return AuthenticatedUser(id = it) }

        return resolveUserIdMutex.withLock {
            // Re-check: another caller may have resolved it while we were waiting for the lock.
            cachedUserId?.let { return@withLock AuthenticatedUser(id = it) }

            val syncedId = storage.get<String>(USER_ID_KEY)
            if (syncedId != null) {
                cachedUserId = syncedId
                return@withLock AuthenticatedUser(id = syncedId)
            }

            auth.currentUser?.takeIf { !it.isAnonymous }?.let { user ->
                Timber.d("Sessão já existente: ${user.uid}")
                crashlytics.setUser(user)
                storage.save(USER_ID_KEY, user.uid)
                cachedUserId = user.uid
                return@withLock user.toAuthenticatedUser()
            }

            throw IllegalStateException("No authenticated user. Sign-in is required.")
        }
    }

    override suspend fun signOut(): Result<Unit> = try {
        storage.remove(USER_ID_KEY)
        cachedUserId = null
        auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAccount(): Result<Unit> = try {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No authenticated user"))
        user.delete().await()
        storage.remove(USER_ID_KEY)
        cachedUserId = null
        Result.success(Unit)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Timber.tag(TAG).e(e, "Falha ao excluir conta")
        Result.failure(e)
    }

    override suspend fun updateUserId(userId: String) {
        Timber.tag(TAG).d("Updating user ID to: $userId (Linking account)")
        storage.save(USER_ID_KEY, userId)
        cachedUserId = userId
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        id = uid,
        email = email,
        displayName = displayName,
        phoneNumber = phoneNumber,
        photoUrl = photoUrl?.toString()
    )

    // Anonymous Firebase sessions (including ones created before this app version dropped
    // anonymous sign-in) are never treated as an authenticated identity: the app requires
    // sign-in with a real provider (e.g. Google).
    private fun FirebaseUser.toAuthenticatedUserOrNull() =
        takeIf { !it.isAnonymous }?.toAuthenticatedUser()

    private fun FirebaseCrashlytics.setUser(user: FirebaseUser?) {
        user?.let {
            setUserId(it.uid)
            setCustomKey("user_id", it.uid)
        }
    }

    companion object {
        private const val TAG = "FirebaseAnonymousAuthentication"
        private const val USER_ID_KEY = "synced_user_id"
    }
}
