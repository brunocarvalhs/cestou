package br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackState

/** Memória local da política. Leitura e escrita síncronas, sem suspender a UI. */
internal interface FeedbackStateStore {
    fun load(): FeedbackState
    fun recordSession()
    fun recordPurchaseFinished()
    fun recordShown(prompt: FeedbackPrompt)
    fun recordAnswered(prompt: FeedbackPrompt, score: Int)
    fun recordDismissed(prompt: FeedbackPrompt)
    fun recordStoreReviewRequested()
}
