package br.com.brunocarvalhs.howmuch.core.domain.exception

import br.com.brunocarvalhs.howmuch.core.common.exception.AppException

/**
 * Base para violações de regra de negócio esperadas (ex: usuário sem permissão, estado
 * inválido para a ação) — diferente de um erro técnico inesperado, é algo que a UI deve
 * tratar mostrando uma mensagem ao usuário, não necessariamente um bug a investigar.
 *
 * Ainda estende [AppException] para que, se um [BusinessRuleException] específico escapar
 * sem tratamento em algum ponto, ele continue rastreável no Crashlytics pelo [tag].
 */
open class BusinessRuleException(
    tag: String,
    message: String? = null,
    cause: Throwable? = null
) : AppException(tag = tag, message = message, cause = cause)
