package br.com.brunocarvalhs.howmuch.session

import kotlinx.coroutines.CoroutineScope

/**
 * Mantém os efeitos da sessão do Cestou em sincronia com a conta logada: o perfil do usuário no
 * Firestore e a limpeza do relógio e da IA quando o usuário sai.
 */
internal interface SessionSync {

    /** Começa a observar a sessão no [scope] (normalmente o `viewModelScope` da Activity). */
    fun start(scope: CoroutineScope)
}
