package br.com.brunocarvalhs.data.di

import android.content.Context
import br.com.brunocarvalhs.data.repository.ShoppingCartRepositoryImpl
import br.com.brunocarvalhs.data.services.CartLocalStorage
import br.com.brunocarvalhs.data.services.DataStorageService
import br.com.brunocarvalhs.data.services.GooglePlaySubscriptionService
import br.com.brunocarvalhs.data.services.RealtimeService
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.Database
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.services.IDataStorageService
import br.com.brunocarvalhs.domain.services.SubscriptionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModuleDI {

    @Provides
    @Singleton
    fun provideRealtimeService(): Database {
        return RealtimeService()
    }

    @Provides
    @Singleton
    fun provideDatabaseService(@ApplicationContext context: Context): IDataStorageService {
        return DataStorageService(context = context)
    }

    @Provides
    @Singleton
    fun provideCartLocalStorage(dataStorageService: IDataStorageService): ICartLocalStorage {
        return CartLocalStorage(dataStorageService = dataStorageService)
    }

    @Provides
    @Singleton
    fun provideSubscriptionService(@ApplicationContext context: Context): SubscriptionService {
        return GooglePlaySubscriptionService(context = context)
    }

    @Provides
    fun provideShoppingCartRepository(service: Database): ShoppingCartRepository {
        return ShoppingCartRepositoryImpl(service)
    }
}
