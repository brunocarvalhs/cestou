package br.com.brunocarvalhs.howmuch.wear.di

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

/**
 * `:wear` não depende de `:feature:feedback` (as pesquisas de satisfação são só do celular, sdd-034),
 * mas os `@HiltViewModel` de `feature:cart`, `feature:chat` e `feature:products` que ele agrega
 * injetam [FeedbackPromptRequester]. Sem este binding o Hilt falha com `[Dagger/MissingBinding]`
 * (mesmo caso do `WearSubscriptionModule`). No relógio nada é perguntado.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object WearFeedbackModule {

    @Provides
    @Singleton
    fun provideFeedbackPromptRequester(): FeedbackPromptRequester = object : FeedbackPromptRequester {
        override val pending: StateFlow<FeedbackPrompt?> = MutableStateFlow(null)
        override fun onAppForeground() = Unit
        override fun onMoment(moment: FeedbackMoment) = Unit
        override fun consume(prompt: FeedbackPrompt) = Unit
        override fun isFeedbackEnabled(): Boolean = false
    }
}
