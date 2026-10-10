package br.com.brunocarvalhs.howmuch.feature.chat.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.brunocarvalhs.howmuch.core.data.service.DataStoreStorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.chat.ChatNavGraph
import br.com.brunocarvalhs.howmuch.feature.chat.data.repository.ChatHistoryRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChatDataStore

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ChatModule {

    @Binds
    @IntoSet
    abstract fun bindChatNavGraph(impl: ChatNavGraph): FeatureNavGraph

    @Binds
    abstract fun bindChatHistoryRepository(impl: ChatHistoryRepositoryImpl): ChatHistoryRepository

    companion object {
        @Provides
        @Singleton
        @ChatDataStore
        fun provideChatDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("ai_chat_prefs") }
            )

        @Provides
        @Singleton
        @ChatDataStore
        fun provideChatStorageService(
            @ChatDataStore dataStore: DataStore<Preferences>
        ): StorageService = DataStoreStorageService(dataStore)
    }
}
