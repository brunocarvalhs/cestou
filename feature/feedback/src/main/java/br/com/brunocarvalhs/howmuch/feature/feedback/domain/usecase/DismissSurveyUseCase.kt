package br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import javax.inject.Inject

/** Fechou sem responder: aplica o backoff (14/45/90 dias). Não conta como resposta. */
internal class DismissSurveyUseCase @Inject constructor(private val store: FeedbackStateStore) {
    operator fun invoke(prompt: FeedbackPrompt) = store.recordDismissed(prompt)
}
