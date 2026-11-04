package br.com.brunocarvalhs.howmuch.feature.chat.presentation.state

/** Voto binário numa resposta do assistente; [value] é o que vai ao analytics. */
enum class Vote(val value: String) { UP("up"), DOWN("down") }

/** Motivo do 👎, de lista fechada: nenhum texto livre sai do aparelho. */
enum class ReasonChip(val value: String) {
    WRONG_ANSWER("wrong_answer"),
    NOT_UNDERSTOOD("not_understood"),
    TOO_SLOW("too_slow"),
    DID_NOTHING("did_nothing"),
    OTHER("other")
}

data class MessageVote(val value: Vote, val reason: ReasonChip? = null)

/** Quem respondeu e quantas ferramentas rodaram no turno; só em memória, anexado ao voto. */
data class AiTurnInfo(val provider: String, val toolCalls: Int)
