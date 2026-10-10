package br.com.brunocarvalhs.howmuch.feature.products.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.products.ProductsNavGraph
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductsModule {

    @Binds
    @IntoSet
    abstract fun bindProductsNavGraph(impl: ProductsNavGraph): FeatureNavGraph
}
