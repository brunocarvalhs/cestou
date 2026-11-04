package br.com.brunocarvalhs.howmuch.core.domain.model

/** Instante do uso em que o app pode pedir a opinião do cliente (ver `FeedbackPromptRequester`). */
enum class FeedbackMoment {
    PURCHASE_FINISHED,
    AI_CHAT_CLOSED,
    PHOTO_SCAN_CONFIRMED,
    APP_RELATIONSHIP
}
