package br.com.brunocarvalhs.howmuch.feature.shopping.commons.di

import br.com.brunocarvalhs.howmuch.feature.shopping.ShoppingInitializer
import br.com.brunocarvalhs.howmuch.feature.shopping.ShoppingInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShoppingModule {

    @Binds
    @IntoSet
    abstract fun bindShoppingInitializer(impl: ShoppingInitializerImpl): br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
}
