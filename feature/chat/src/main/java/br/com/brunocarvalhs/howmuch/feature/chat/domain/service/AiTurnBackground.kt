package br.com.brunocarvalhs.howmuch.feature.chat.domain.service

/**
 * Tudo o que mantém a IA trabalhando com o app fora de vista: o serviço em primeiro plano e as
 * notificações. Público porque o `AiChatViewModel` o recebe por construtor.
 */
interface AiTurnBackground {

    /** Um turno da IA começou: liga o serviço que mantém o processo vivo. */
    fun turnStarted()

    /**
     * O turno acabou. [result] é a mensagem final da IA, ou `null` se o turno foi cancelado. Se o
     * usuário não está olhando o chat, avisa com uma notificação que abre a conversa [sessionId].
     */
    fun turnFinished(sessionId: String?, result: String?)

    /** A IA espera uma aprovação; fora do chat, avisa com uma notificação que abre o card. */
    fun approvalNeeded(sessionId: String?)

    /** O chat apareceu ou saiu de vista; aparecer limpa as notificações pendentes. */
    fun setChatVisible(visible: Boolean)
}
