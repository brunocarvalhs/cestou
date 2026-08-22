package br.com.brunocarvalhs.howmuch.feature.products.commons.di

import br.com.brunocarvalhs.howmuch.feature.products.ProductsInitializer
import br.com.brunocarvalhs.howmuch.feature.products.ProductsInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProductsModule {

    @Binds
    @IntoSet
    abstract fun bindProductsInitializer(impl: ProductsInitializerImpl): br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
}
