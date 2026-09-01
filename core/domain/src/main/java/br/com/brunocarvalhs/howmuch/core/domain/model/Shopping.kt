package br.com.brunocarvalhs.howmuch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Shopping(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val status: Status,
    val users: List<String>,
    val roles: Map<String, String>,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isCategorized: Boolean = true,
    val shortCode: String? = null,
    val budget: Double? = null,
    val position: Int = 0,
    val emoji: String = DEFAULT_EMOJI
) {
    @Serializable
    enum class Status {
        NEW,
        IN_PROGRESS,
        FINISH
    }

    companion object {
        const val DEFAULT_EMOJI = "🛒"
    }
}
