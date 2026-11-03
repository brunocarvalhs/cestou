package br.com.brunocarvalhs.howmuch.core.domain.model

/** What happened to a received push. [Failed] carries the cause for logging, never message content. */
sealed interface PushSaveOutcome {
    data object Saved : PushSaveOutcome
    data object AlreadySaved : PushSaveOutcome
    data class Ignored(val reason: IgnoreReason) : PushSaveOutcome
    data class Failed(val cause: Throwable) : PushSaveOutcome

    enum class IgnoreReason { NO_USER, EMPTY }
}
