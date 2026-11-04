package br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import javax.inject.Inject

/** Registra a resposta do NPS (0-10): inicia o cooldown e, se a nota for baixa, a supressão da avaliação na loja. */
internal class SubmitNpsUseCase @Inject constructor(private val store: FeedbackStateStore) {
    operator fun invoke(prompt: FeedbackPrompt, score: Int) = store.recordAnswered(prompt, score)
}
