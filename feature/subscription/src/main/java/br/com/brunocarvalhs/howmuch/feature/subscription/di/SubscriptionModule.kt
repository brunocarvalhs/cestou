package br.com.brunocarvalhs.howmuch.feature.subscription.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
import br.com.brunocarvalhs.howmuch.feature.subscription.SubscriptionInitializer
import br.com.brunocarvalhs.howmuch.feature.subscription.SubscriptionInitializerImpl
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
    abstract fun bindSubscriptionInitializer(impl: SubscriptionInitializerImpl): FeatureInitializer
}
