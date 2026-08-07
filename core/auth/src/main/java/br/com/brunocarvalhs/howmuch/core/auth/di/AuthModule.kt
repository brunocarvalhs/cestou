package br.com.brunocarvalhs.howmuch.core.auth.di

import br.com.brunocarvalhs.howmuch.core.auth.FirebaseAnonymousAuthentication
import br.com.brunocarvalhs.howmuch.core.domain.service.AuthService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthService(impl: FirebaseAnonymousAuthentication): AuthService = impl

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth
}
