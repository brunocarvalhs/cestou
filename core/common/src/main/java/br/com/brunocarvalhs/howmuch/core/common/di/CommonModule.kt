package br.com.brunocarvalhs.howmuch.core.common.di

import br.com.brunocarvalhs.howmuch.core.common.contract.AppVersionProvider
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.common.service.FirebaseCrashReporter
import br.com.brunocarvalhs.howmuch.core.common.service.PackageManagerVersionProvider
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter

    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(impl: PackageManagerVersionProvider): AppVersionProvider

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseCrashlytics(): FirebaseCrashlytics = Firebase.crashlytics
    }
}
