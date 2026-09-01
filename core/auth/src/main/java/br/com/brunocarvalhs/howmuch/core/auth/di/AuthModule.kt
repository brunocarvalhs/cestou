package br.com.brunocarvalhs.howmuch.core.auth.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.brunocarvalhs.howmuch.core.auth.FirebaseAnonymousAuthentication
import br.com.brunocarvalhs.howmuch.core.data.service.DataStoreStorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthDataStore

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthService(impl: FirebaseAnonymousAuthentication): AuthService = impl

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    @AuthDataStore
    fun provideAuthDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") }
        )
    }

    @Provides
    @Singleton
    @AuthDataStore
    fun provideAuthStorageService(
        @AuthDataStore dataStore: DataStore<Preferences>
    ): StorageService = DataStoreStorageService(dataStore)
}
