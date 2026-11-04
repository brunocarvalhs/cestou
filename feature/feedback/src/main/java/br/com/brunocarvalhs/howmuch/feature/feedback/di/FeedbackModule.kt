package br.com.brunocarvalhs.howmuch.feature.feedback.di

import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.feedback.FeedbackNavGraph
import br.com.brunocarvalhs.howmuch.feature.feedback.data.store.FeedbackStateStoreImpl
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.coordinator.FeedbackPromptCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeedbackModule {

    @Binds
    @IntoSet
    abstract fun bindFeedbackNavGraph(impl: FeedbackNavGraph): FeatureNavGraph

    @Binds
    abstract fun bindFeedbackPromptRequester(impl: FeedbackPromptCoordinator): FeedbackPromptRequester

    @Binds
    abstract fun bindFeedbackStateStore(impl: FeedbackStateStoreImpl): FeedbackStateStore
}
