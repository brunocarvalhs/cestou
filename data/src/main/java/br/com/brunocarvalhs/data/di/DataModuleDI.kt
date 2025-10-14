package br.com.brunocarvalhs.data.di

import android.content.Context
import br.com.brunocarvalhs.data.repository.ShoppingCartRepositoryImpl
import br.com.brunocarvalhs.data.services.CartLocalStorage
import br.com.brunocarvalhs.data.services.DataStorageService
import br.com.brunocarvalhs.data.services.RealtimeService
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.services.IDataStorageService
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
    fun provideRealtimeService(): RealtimeService {
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
    fun provideShoppingCartRepository(service: RealtimeService): ShoppingCartRepository {
        return ShoppingCartRepositoryImpl(service)
    }
}
