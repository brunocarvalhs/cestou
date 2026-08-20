package br.com.brunocarvalhs.howmuch.core.data.di

import br.com.brunocarvalhs.howmuch.core.data.cloud.CloudNetwork
import br.com.brunocarvalhs.howmuch.core.data.network.CompatibilityConverter
import br.com.brunocarvalhs.howmuch.core.data.network.FirebaseFirestoreManager
import br.com.brunocarvalhs.howmuch.core.data.network.NetworkLogger
import br.com.brunocarvalhs.howmuch.core.data.network.NetworkManager
import br.com.brunocarvalhs.howmuch.core.data.security.CryptoManager
import br.com.brunocarvalhs.howmuch.core.data.repository.NotificationRepositoryImpl
import br.com.brunocarvalhs.howmuch.core.data.repository.UserRepositoryImpl
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.service.NetworkService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

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
