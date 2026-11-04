package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.intent

internal data class FeedbackSurveyIntent(
    val onScoreSelected: (Int) -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onContactSupport: () -> Unit = {},
)
