package br.com.brunocarvalhs.howmuch.feature.shopping.app.data.model

import br.com.brunocarvalhs.howmuch.core.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    override val id: String,
    override val role: User.Role = User.Role.EDITOR
) : User {

    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "ID" to id,
            "ROLE" to role.name
        )
    }


    companion object {
        fun fromMap(map: Map<String, Any?>): UserModel {
            return UserModel(
                id = map["ID"] as? String ?: error("Campo 'id' é obrigatório"),
                role = (map["ROLE"] as? String)?.let(User.Role::valueOf) ?: User.Role.EDITOR
            )
        }
    }
}
