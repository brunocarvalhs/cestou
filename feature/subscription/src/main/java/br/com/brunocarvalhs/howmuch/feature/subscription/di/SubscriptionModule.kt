package br.com.brunocarvalhs.howmuch.feature.subscription.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.subscription.SubscriptionNavGraph
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SubscriptionModule {

    @Binds
    @IntoSet
    abstract fun bindSubscriptionNavGraph(impl: SubscriptionNavGraph): FeatureNavGraph
}
