package br.com.brunocarvalhs.howmuch.feature.shopping.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.brunocarvalhs.howmuch.core.data.service.DataStoreStorageService
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.feature.shopping.data.repository.ShoppingRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.shopping.data.text.ResourceShoppingTexts
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.ShoppingTexts
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WearListDataStore

@Module
@InstallIn(SingletonComponent::class)
abstract class ShoppingDataModule {

    @Binds
    @Singleton
    abstract fun bindShoppingRepository(impl: ShoppingRepositoryImpl): ShoppingRepository

    @Binds
    internal abstract fun bindShoppingTexts(impl: ResourceShoppingTexts): ShoppingTexts

    companion object {
        @Provides
        @Singleton
        @WearListDataStore
        fun provideWearListDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("wear_list_prefs") }
            )

        @Provides
        @Singleton
        @WearListDataStore
        fun provideWearListStorageService(
            @WearListDataStore dataStore: DataStore<Preferences>
        ): StorageService = DataStoreStorageService(dataStore)
    }
}
