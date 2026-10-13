package br.com.brunocarvalhs.howmuch.feature.profile.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.profile.ProfileNavGraph
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProfileModule {

    @Binds
    @IntoSet
    abstract fun bindProfileNavGraph(impl: ProfileNavGraph): FeatureNavGraph
}
