package br.com.brunocarvalhs.howmuch.update

/**
 * Estado do force update (sdd-059). Decidido por [ForceUpdateController.check] a partir da versão
 * mínima remota e do que o Play oferece.
 */
internal sealed interface ForceUpdateState {

    /** O app está na versão mínima exigida (ou não há mínimo configurado). Segue normalmente. */
    data object Idle : ForceUpdateState

    /**
     * Versão abaixo do mínimo e o Play oferece atualização IMMEDIATE. [resume] indica que uma
     * atualização já estava em andamento e deve ser retomada.
     */
    data class Required(val resume: Boolean) : ForceUpdateState

    /** Versão abaixo do mínimo, mas o Play não oferece atualização (ex.: instalação fora da Play). */
    data object Blocked : ForceUpdateState
}
