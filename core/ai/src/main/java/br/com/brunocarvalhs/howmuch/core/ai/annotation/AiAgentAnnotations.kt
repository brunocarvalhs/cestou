package br.com.brunocarvalhs.howmuch.core.ai.annotation

/**
 * Anotação para definir uma ação que o Agente de IA pode executar.
 */
@Target(AnnotationTarget.CLASS)
annotation class AiAction(
    val id: String,
    val description: String
)

/**
 * Anotação para definir os parâmetros de uma ação da IA.
 */
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class AiParam(
    val name: String,
    val type: String = "string",
    val description: String,
    val isRequired: Boolean = true
)
