package br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.exception

import br.com.brunocarvalhs.howmuch.core.domain.exception.BusinessRuleException

/**
 * Lançada quando um usuário sem o papel [br.com.brunocarvalhs.howmuch.core.domain.model.User.Role.OWNER]
 * tenta executar uma ação restrita ao dono da lista (ex: excluir a lista).
 */
class OwnershipRequiredException(
    action: String
) : BusinessRuleException(tag = TAG, message = "Ação '$action' requer papel de owner na lista") {

    companion object {
        private const val TAG = "shopping_ownership"
    }
}
