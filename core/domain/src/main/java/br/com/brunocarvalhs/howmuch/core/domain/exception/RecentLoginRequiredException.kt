package br.com.brunocarvalhs.howmuch.core.domain.exception

/** A sensitive operation (e.g. account deletion) needs a fresh sign-in before it can proceed. */
class RecentLoginRequiredException(cause: Throwable? = null) :
    AppException(tag = "auth_recent_login", message = "Recent login required", cause = cause)
