package br.com.brunocarvalhs.howmuch.feature.settings.di

import br.com.brunocarvalhs.howmuch.core.domain.services.ReminderScheduler
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.settings.SettingsNavGraph
import br.com.brunocarvalhs.howmuch.feature.settings.data.worker.ShoppingReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsModule {

    @Binds
    @IntoSet
    abstract fun bindSettingsNavGraph(impl: SettingsNavGraph): FeatureNavGraph

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: ShoppingReminderScheduler): ReminderScheduler
}
