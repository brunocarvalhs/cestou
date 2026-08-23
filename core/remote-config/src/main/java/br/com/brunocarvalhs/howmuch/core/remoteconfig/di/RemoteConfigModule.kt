package br.com.brunocarvalhs.howmuch.core.remoteconfig.di

import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteConfigSyncService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.service.FirebaseRemoteConfigService
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {

    @Binds
    @Singleton
    abstract fun bindFeatureFlagService(impl: FirebaseRemoteConfigService): FeatureFlagService

    @Binds
    @Singleton
    abstract fun bindRemoteVariableService(impl: FirebaseRemoteConfigService): RemoteVariableService

    @Binds
    @Singleton
    abstract fun bindRemoteConfigSyncService(impl: FirebaseRemoteConfigService): RemoteConfigSyncService

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = Firebase.remoteConfig
    }
}
