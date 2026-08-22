package br.com.brunocarvalhs.howmuch.feature.settings.commons.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.brunocarvalhs.howmuch.feature.settings.app.data.repository.SettingsRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.SettingsInitializer
import br.com.brunocarvalhs.howmuch.feature.settings.SettingsInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @IntoSet
    abstract fun bindSettingsInitializer(impl: SettingsInitializerImpl): br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer

    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("settings") }
            )
        }
    }
}
