package br.com.brunocarvalhs.howmuch.feature.shopping.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.shopping.ShoppingNavGraph
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ShoppingModule {

    @Binds
    @IntoSet
    abstract fun bindShoppingNavGraph(impl: ShoppingNavGraph): FeatureNavGraph
}
