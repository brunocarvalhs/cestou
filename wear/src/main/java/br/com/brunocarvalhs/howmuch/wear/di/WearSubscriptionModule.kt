package br.com.brunocarvalhs.howmuch.wear.di

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

/**
 * `:wear` doesn't depend on `core:billing` (no Play Billing on the watch — AD-010 is phone-only),
 * but several `@HiltViewModel` classes it pulls in transitively (`ProfileSubscriptionViewModel`,
 * `AiAgentFactoryImpl`, the `*SubscriptionGateViewModel`s in feature:cart/chat/shopping) inject
 * [SubscriptionRepository]. Every `@HiltViewModel` in a module `:wear` depends on is aggregated
 * into `:wear`'s own Hilt component regardless of whether wear's UI ever requests it — Dagger's
 * `hiltJavaCompileDebug` failed with `[Dagger/MissingBinding]` without this, even though nothing
 * on the watch actually calls any of those classes. Always-FREE is also the semantically correct
 * answer here: there is no purchase flow on the watch.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object WearSubscriptionModule {

    @Provides
    @Singleton
    fun provideSubscriptionRepository(): SubscriptionRepository = object : SubscriptionRepository {
        override fun observeStatus(): Flow<SubscriptionStatus> = flowOf(SubscriptionStatus.FREE)
    }
}
