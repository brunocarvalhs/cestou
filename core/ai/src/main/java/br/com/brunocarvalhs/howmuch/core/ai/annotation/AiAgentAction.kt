package br.com.brunocarvalhs.howmuch.core.ai.annotation

@Target(AnnotationTarget.CLASS)
annotation class AiAgentAction(
    val id: String,
    val description: String
)

@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class AiAgentParameter(
    val name: String,
    val type: String = "string",
    val description: String,
    val isRequired: Boolean = true
)
