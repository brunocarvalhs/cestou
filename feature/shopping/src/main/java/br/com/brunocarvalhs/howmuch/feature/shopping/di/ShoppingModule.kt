package br.com.brunocarvalhs.howmuch.feature.shopping.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.shopping.ShoppingNavGraph
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.SPENDING_IO_DISPATCHER
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
abstract class ShoppingModule {

    @Binds
    @IntoSet
    abstract fun bindShoppingNavGraph(impl: ShoppingNavGraph): FeatureNavGraph

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Named(SPENDING_IO_DISPATCHER)
        fun provideSpendingIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
