package br.com.brunocarvalhs.howmuch.core.domain.util

import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError.CONTACT_INFO
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError.EMPTY
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError.FULL_NAME_TOO_LONG
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError.INVALID_CHARACTERS
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError.NOT_A_NAME
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError.TOO_LONG
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError.TOO_SHORT
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** Covers spec EPA-13 (`.specs/features/email-phone-auth/spec.md`), items 1-11 and 18. */
@Suppress("TooManyFunctions", "MagicNumber")
class PersonNameValidatorTest {

    private fun valid(given: String, family: String): PersonNameResult.Valid =
        when (val result = PersonNameValidator.validate(given, family)) {
            is PersonNameResult.Valid -> result
            is PersonNameResult.Invalid -> {
                fail("Expected \"$given\" / \"$family\" to be accepted, got $result")
                error("unreachable")
            }
        }

    private fun invalid(given: String, family: String): PersonNameResult.Invalid =
        when (val result = PersonNameValidator.validate(given, family)) {
            is PersonNameResult.Invalid -> result
            is PersonNameResult.Valid -> {
                fail("Expected \"$given\" / \"$family\" to be rejected, got ${result.fullName}")
                error("unreachable")
            }
        }

    // region item 9 - must accept

    @Test
    fun `accepts every real name from the spec false-positive list`() {
        listOf(
            "Li" to "Wu", "Jô" to "Silva", "Ana" to "Souza", "Bo" to "Wu", "Noor" to "Ali",
            "João Pedro" to "Santos", "Ana-Clara" to "Lima", "Maria" to "da Silva",
            "José" to "D'Ávila", "Sean" to "O'Neil", "Nguyen" to "Thi Hoa", "Hans" to "Müller",
            "Åsa" to "Berg", "Raoni" to "Metuktire", "Raoni" to "Raoni", "Jussara" to "Tupinambá",
            "Aaron" to "Anna", "Nuno" to "Álvares", "Ye" to "Zé", "李雷" to "韩梅", "Nada" to "Silva",
            "Asdrúbal" to "Souza", "Cassandra" to "Lima", "Analisa" to "Costa",
        ).forEach { (given, family) -> valid(given, family) }
    }

    // endregion

    // region item 10 - must reject

    @Test
    fun `rejects empty and blank fields on the field that is empty`() {
        assertEquals(EMPTY, invalid("", "Silva").givenNameError)
        assertEquals(EMPTY, invalid("Ana", "").familyNameError)
        val blank = invalid("   ", "   ")
        assertEquals(EMPTY, blank.givenNameError)
        assertEquals(EMPTY, blank.familyNameError)
    }

    @Test
    fun `rejects single letter fields as too short`() {
        assertEquals(TOO_SHORT, invalid("A", "Silva").givenNameError)
        assertEquals(TOO_SHORT, invalid("Ana", "S").familyNameError)
    }

    @Test
    fun `rejects repeated letters, keyboard sequences and placeholders as not a name`() {
        assertEquals(NOT_A_NAME, invalid("aaaa", "Silva").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Ana", "xxx").familyNameError)
        assertEquals(NOT_A_NAME, invalid("qwerty", "Silva").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Teste", "Teste").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Fulano", "de Tal").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Test", "User").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Usuario", "Silva").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Nome", "Sobrenome").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Admin", "Admin").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Silva", "Teste").familyNameError)
        assertEquals(NOT_A_NAME, invalid("Usuário", "Silva").givenNameError)
    }

    @Test
    fun `rejects a keyboard sequence split across both fields on the family name`() {
        assertEquals(NOT_A_NAME, invalid("asdf", "ghjk").givenNameError)
        val splitSequence = invalid("Asd", "Fgh")
        assertEquals(null, splitSequence.givenNameError)
        assertEquals(NOT_A_NAME, splitSequence.familyNameError)
    }

    @Test
    fun `rejects full-name placeholders on the family name`() {
        assertEquals(NOT_A_NAME, invalid("John", "Doe").familyNameError)
        assertEquals(NOT_A_NAME, invalid("Juan", "Perez").familyNameError)
    }

    @Test
    fun `rejects digits, emojis and symbols as invalid characters`() {
        assertEquals(INVALID_CHARACTERS, invalid("joao123", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("João 2", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("😀", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("João😀", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("--", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("-Ana", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("Ana-", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("Ana--Clara", "Silva").givenNameError)
        assertEquals(INVALID_CHARACTERS, invalid("Ana_Clara", "Silva").givenNameError)
    }

    @Test
    fun `rejects e-mail, links and phone numbers as contact info`() {
        assertEquals(CONTACT_INFO, invalid("maria@gmail.com", "Silva").givenNameError)
        assertEquals(CONTACT_INFO, invalid("www.site.com", "Silva").givenNameError)
        assertEquals(CONTACT_INFO, invalid("Ana", "http://x.io").familyNameError)
        assertEquals(CONTACT_INFO, invalid("11999998888", "Silva").givenNameError)
        assertEquals(CONTACT_INFO, invalid("(11) 99999-8888", "Silva").givenNameError)
    }

    @Test
    fun `rejects fields with more than 3 words`() {
        assertEquals(TOO_LONG, invalid("Abelardo Bonifacio Crisostomo Damasceno", "Silva").givenNameError)
        assertEquals(TOO_LONG, invalid("Ana", "Bia Cris Duda Lima").familyNameError)
    }

    @Test
    fun `rejects a 31-letter field`() {
        val thirtyOne = "MarianaTerezaTerezaTerezaLucasA"
        assertEquals(31, thirtyOne.length)
        assertEquals(TOO_LONG, invalid(thirtyOne, "Silva").givenNameError)
    }

    @Test
    fun `accepts a 30-letter field`() {
        valid("MarianaTerezaTerezaTerezaLucas", "Silva")
    }

    @Test
    fun `rejects a full name over 60 characters on the family name`() {
        val given = "MarianaTerezaTerezaTerezaLucas" // 30 letters
        val family = "Albuquerque Moraes Vasconcelos" // 30 chars, 3 words
        val result = invalid(given, family)
        assertEquals(null, result.givenNameError)
        assertEquals(FULL_NAME_TOO_LONG, result.familyNameError)
    }

    @Test
    fun `rejects a profanity in either field by whole word`() {
        assertEquals(NOT_A_NAME, invalid("Merda", "Silva").givenNameError)
        assertEquals(NOT_A_NAME, invalid("Ana", "Fuck").familyNameError)
        assertEquals(NOT_A_NAME, invalid("Ana", "Puta Silva").familyNameError)
    }

    @Test
    fun `reports errors for both fields at once`() {
        val result = invalid("", "aaaa")
        assertEquals(EMPTY, result.givenNameError)
        assertEquals(NOT_A_NAME, result.familyNameError)
    }

    // endregion

    // region item 1 and 18 - normalization and concatenation

    @Test
    fun `normalizes spacing and capitalization and concatenates with a single space`() {
        val result = valid("  maRIA ", " da   silva")
        assertEquals("Maria", result.givenName)
        assertEquals("Da Silva", result.familyName)
        assertEquals("Maria Da Silva", result.fullName)
    }

    @Test
    fun `keeps particles lowercase when they are not the first word of a field`() {
        assertEquals("Silva dos Santos", valid("João", "silva dos santos").familyName)
        assertEquals("Maria da Graça", valid("maria da graça", "Lima").givenName)
    }

    @Test
    fun `preserves internal capitals the user typed and capitalizes hyphenated parts`() {
        assertEquals("O'Neil", valid("Sean", "O'Neil").familyName)
        assertEquals("McDonald", valid("Ronald", "McDonald").familyName)
        assertEquals("Ana-Clara", valid("ana-clara", "Lima").givenName)
    }

    @Test
    fun `normalizes typographic apostrophe to a plain one`() {
        assertEquals("D'Ávila", valid("José", "D’Ávila").familyName)
    }

    // endregion
}
