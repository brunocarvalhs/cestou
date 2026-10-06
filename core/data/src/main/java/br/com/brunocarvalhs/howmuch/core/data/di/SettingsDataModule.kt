package br.com.brunocarvalhs.howmuch.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.brunocarvalhs.howmuch.core.data.repository.SettingsRepositoryImpl
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Split out from [DataModule] so `SettingsRepository`'s binding (promoted from `feature/settings`,
 * see `.specs/features/g10-cross-feature-decoupling/`) has its own module, mirroring how
 * `feature/settings`'s original `SettingsModule` isolated it.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsDataModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

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
