package br.com.brunocarvalhs.howmuch.core.data.extensions

import br.com.brunocarvalhs.howmuch.core.data.model.UserProfileModel
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileMapperTest {

    @Test
    fun `toDomain maps every field from UserProfileModel to UserProfile`() {
        val model = UserProfileModel(id = "u1", name = "Ana", email = "ana@test.com", photoUrl = "http://x/y.png")

        val profile = model.toDomain()

        assertEquals(model.id, profile.id)
        assertEquals(model.name, profile.name)
        assertEquals(model.email, profile.email)
        assertEquals(model.photoUrl, profile.photoUrl)
    }

    @Test
    fun `toModel maps every field from UserProfile to UserProfileModel`() {
        val profile = UserProfile(id = "u1", name = "Ana", email = "ana@test.com", photoUrl = "http://x/y.png")

        val model = profile.toModel()

        assertEquals(profile.id, model.id)
        assertEquals(profile.name, model.name)
        assertEquals(profile.email, model.email)
        assertEquals(profile.photoUrl, model.photoUrl)
    }

    @Test
    fun `toMap exposes every UserProfileModel field by key`() {
        val model = UserProfileModel(id = "u1", name = "Ana", email = "ana@test.com", photoUrl = null)

        val map = model.toMap()

        assertEquals(
            mapOf("id" to "u1", "name" to "Ana", "email" to "ana@test.com", "photoUrl" to null),
            map
        )
    }
}
