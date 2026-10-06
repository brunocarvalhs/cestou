package br.com.brunocarvalhs.howmuch.core.billing.di

import br.com.brunocarvalhs.howmuch.core.billing.PlayBillingSubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [SubscriptionRepository] to its Play Billing implementation. Installed in
 * [SingletonComponent] like every other core repository binding (see `core:auth`'s
 * `AuthModule`) — Hilt picks this module up because `:app` depends on `core:billing` (AD-010:
 * "No other module depends on it").
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: PlayBillingSubscriptionRepository
    ): SubscriptionRepository
}
