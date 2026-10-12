package br.com.brunocarvalhs.howmuch.core.data.di

import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.data.service.FirebaseCrashReporter
import br.com.brunocarvalhs.howmuch.core.data.wearable.WearableSyncServiceImpl
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Implementations of core:common contracts that need Firebase or Play Services; the contracts
// stay in core:common so its consumers don't pull those SDKs.
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter

    @Binds
    @Singleton
    internal abstract fun bindWearableSyncService(impl: WearableSyncServiceImpl): WearableSyncService

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseCrashlytics(): FirebaseCrashlytics = Firebase.crashlytics
    }
}
