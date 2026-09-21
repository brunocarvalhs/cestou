package br.com.brunocarvalhs.howmuch.feature.shopping.data.model

import br.com.brunocarvalhs.howmuch.core.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * `UserModel` is how each member of a shared shopping list (owner + collaborators) is persisted
 * inside Firestore's `roles`/`users` maps for the list-sharing/join flow. A broken `toMap`/`fromMap`
 * round-trip here silently corrupts who owns vs. who can only edit a shared list.
 */
class UserModelTest {

    @Test
    fun `toMap serializes id and role`() {
        val model = UserModel(id = "user-1", role = User.Role.OWNER)

        val map = model.toMap()

        assertEquals("user-1", map["ID"])
        assertEquals("OWNER", map["ROLE"])
    }

    @Test
    fun `fromMap reconstructs the model produced by toMap`() {
        val original = UserModel(id = "user-2", role = User.Role.EDITOR)

        val restored = UserModel.fromMap(original.toMap())

        assertEquals(original, restored)
    }

    @Test
    fun `fromMap defaults to EDITOR role when role is missing`() {
        val restored = UserModel.fromMap(mapOf("ID" to "user-3"))

        assertEquals(User.Role.EDITOR, restored.role)
    }

    @Test
    fun `fromMap throws when id is missing`() {
        assertThrows(IllegalStateException::class.java) {
            UserModel.fromMap(mapOf("ROLE" to "OWNER"))
        }
    }

    @Test
    fun `constructor defaults role to EDITOR`() {
        val model = UserModel(id = "user-4")

        assertEquals(User.Role.EDITOR, model.role)
    }
}
