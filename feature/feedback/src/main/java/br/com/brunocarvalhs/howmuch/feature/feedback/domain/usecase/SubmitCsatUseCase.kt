package br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import javax.inject.Inject

/** Registra a resposta do CSAT (1-5) do recurso em `prompt.feature`: inicia o cooldown daquele recurso. */
internal class SubmitCsatUseCase @Inject constructor(private val store: FeedbackStateStore) {
    operator fun invoke(prompt: FeedbackPrompt, score: Int) = store.recordAnswered(prompt, score)
}
