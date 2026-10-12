package br.com.brunocarvalhs.howmuch.core.common.di

import br.com.brunocarvalhs.howmuch.core.common.contract.AppVersionProvider
import br.com.brunocarvalhs.howmuch.core.common.service.PackageManagerVersionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(impl: PackageManagerVersionProvider): AppVersionProvider
}
