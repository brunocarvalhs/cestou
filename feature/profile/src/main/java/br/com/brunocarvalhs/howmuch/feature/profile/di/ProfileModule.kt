package br.com.brunocarvalhs.howmuch.feature.profile.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
import br.com.brunocarvalhs.howmuch.feature.profile.ProfileInitializer
import br.com.brunocarvalhs.howmuch.feature.profile.ProfileInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileInitializer(impl: ProfileInitializerImpl): ProfileInitializer

    @Binds
    @IntoSet
    abstract fun bindFeatureInitializer(impl: ProfileInitializerImpl): FeatureInitializer
}
