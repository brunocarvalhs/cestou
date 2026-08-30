package br.com.brunocarvalhs.howmuch.feature.auth.di

import br.com.brunocarvalhs.howmuch.feature.auth.AuthInitializer
import br.com.brunocarvalhs.howmuch.feature.auth.AuthInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthModule {

    @Binds
    @IntoSet
    abstract fun bindAuthInitializer(impl: AuthInitializerImpl): br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
}
