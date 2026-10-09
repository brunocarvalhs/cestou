package br.com.brunocarvalhs.howmuch.feature.cart.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.cart.CartNavGraph
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CartModule {

    @Binds
    @IntoSet
    abstract fun bindCartNavGraph(impl: CartNavGraph): FeatureNavGraph
}
