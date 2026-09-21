package br.com.brunocarvalhs.howmuch.core.analytics.di

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirebaseAnalyticsTracker
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(): FirebaseAnalytics = Firebase.analytics
    }
}
