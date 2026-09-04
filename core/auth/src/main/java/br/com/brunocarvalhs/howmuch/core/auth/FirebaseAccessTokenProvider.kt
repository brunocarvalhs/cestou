package br.com.brunocarvalhs.howmuch.core.auth

import br.com.brunocarvalhs.howmuch.core.domain.services.AccessTokenProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAccessTokenProvider @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AccessTokenProvider {

    override suspend fun getToken(forceRefresh: Boolean): String? =
        firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token
}
