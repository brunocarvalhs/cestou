package br.com.brunocarvalhs.howmuch.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.brunocarvalhs.howmuch.core.data.cloud.CloudNetwork
import br.com.brunocarvalhs.howmuch.core.data.network.CompatibilityConverter
import br.com.brunocarvalhs.howmuch.core.data.network.FirebaseFirestoreManager
import br.com.brunocarvalhs.howmuch.core.data.network.NetworkLogger
import br.com.brunocarvalhs.howmuch.core.data.network.NetworkManager
import br.com.brunocarvalhs.howmuch.core.data.repository.AiTrialRepositoryImpl
import br.com.brunocarvalhs.howmuch.core.data.repository.NotificationRepositoryImpl
import br.com.brunocarvalhs.howmuch.core.data.repository.UserRepositoryImpl
import br.com.brunocarvalhs.howmuch.core.data.security.CryptoManager
import br.com.brunocarvalhs.howmuch.core.data.service.DataStoreStorageService
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiTrialDataStore

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindNetworkService(impl: NetworkManager): NetworkService

    @Binds
    @Singleton
    @Named("CloudNetwork")
    abstract fun bindCloudNetworkService(impl: CloudNetwork): NetworkService

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindAiTrialRepository(impl: AiTrialRepositoryImpl): AiTrialRepository

    companion object {
        private const val REQUEST_TIMEOUT_MILLIS = 60_000L
        private const val CONNECT_TIMEOUT_MILLIS = 30_000L
        private const val SOCKET_TIMEOUT_MILLIS = 60_000L

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestoreManager(firestore: FirebaseFirestore): FirebaseFirestoreManager =
            FirebaseFirestoreManager(firestore)

        @Provides
        @Singleton
        fun provideCryptoManager(): CryptoManager = CryptoManager()

        @Provides
        @Singleton
        @AiTrialDataStore
        fun provideAiTrialDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("ai_trial_prefs") }
            )

        @Provides
        @Singleton
        @AiTrialDataStore
        fun provideAiTrialStorageService(
            @AiTrialDataStore dataStore: DataStore<Preferences>
        ): StorageService = DataStoreStorageService(dataStore)

        @Provides
        @Singleton
        fun provideCompatibilityConverter(): CompatibilityConverter = CompatibilityConverter()

        @Provides
        @Singleton
        fun provideNetworkLogger(): NetworkLogger = NetworkLogger()

        @Provides
        @Singleton
        fun provideHttpClient(): HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    coerceInputValues = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
        }
    }
}
