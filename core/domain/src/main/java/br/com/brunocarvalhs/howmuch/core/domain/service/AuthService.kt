package br.com.brunocarvalhs.howmuch.core.domain.service

import br.com.brunocarvalhs.howmuch.core.domain.entity.AuthenticatedUser
import kotlinx.coroutines.flow.Flow

interface AuthService {
    val currentUser: AuthenticatedUser?
    val authState: Flow<AuthenticatedUser?>
    
    suspend fun getOrCreateUserId(): AuthenticatedUser
    
    suspend fun signInAnonymously(): Result<AuthenticatedUser>
    suspend fun signOut(): Result<Unit>
    
    // Provisório para o novo layout (Login Completo)
    suspend fun signInWithGoogle(): Result<AuthenticatedUser>
    suspend fun signInWithApple(): Result<AuthenticatedUser>
}
