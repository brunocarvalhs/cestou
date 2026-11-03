package br.com.brunocarvalhs.howmuch.core.domain.model

/** Tipo do pedido de feedback; [STORE_REVIEW] não tem tela de pesquisa, só abre a avaliação da loja. */
enum class SurveyKind { NPS, CSAT, STORE_REVIEW }

/**
 * Pedido de feedback pendente. [feature] é o recurso medido no CSAT (`purchase`, `ai_chat`,
 * `photo_scan`) e fica nulo no NPS e na avaliação da loja.
 */
data class FeedbackPrompt(val kind: SurveyKind, val feature: String? = null)
