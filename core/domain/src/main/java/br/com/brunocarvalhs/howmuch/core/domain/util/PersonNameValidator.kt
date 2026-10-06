package br.com.brunocarvalhs.howmuch.core.domain.util

import java.text.Normalizer
import java.util.Locale

/** Why a first or last name field was refused. UI maps each value to a localized message. */
enum class PersonNameError {
    EMPTY,
    TOO_SHORT,
    TOO_LONG,

    /** First + last name together exceed [PersonNameValidator.MAX_FULL_NAME_LENGTH]; reported on the last name. */
    FULL_NAME_TOO_LONG,
    INVALID_CHARACTERS,

    /** Looks like an e-mail, a link or a phone number. */
    CONTACT_INFO,

    /**
     * Repeated letters, keyboard sequence, placeholder or profanity. One shared value on purpose:
     * the user sees a single neutral message that neither accuses nor teaches how to bypass it.
     */
    NOT_A_NAME,
}

sealed interface PersonNameResult {
    data class Valid(val givenName: String, val familyName: String) : PersonNameResult {
        /** Exactly what is stored in Firebase Auth `displayName` and `users/{uid}.name`. */
        val fullName: String get() = "$givenName $familyName"
    }

    data class Invalid(
        val givenNameError: PersonNameError?,
        val familyNameError: PersonNameError?,
    ) : PersonNameResult
}

/**
 * Validates and normalizes the required first/last name pair (spec EPA-13). Errs on the side of
 * accepting: a rule only belongs here if it rejects none of the real names in the spec's
 * "must accept" list. It reduces obviously fake names; it is not identity verification.
 */
object PersonNameValidator {

    const val MIN_LETTERS = 2
    const val MAX_LETTERS = 30
    const val MAX_WORDS = 3
    const val MAX_FULL_NAME_LENGTH = 60

    private const val MIN_KEYBOARD_RUN_FIELD = 4
    private const val MIN_KEYBOARD_RUN_FULL_NAME = 6
    private const val MIN_PHONE_DIGITS = 7

    fun validate(givenName: String, familyName: String): PersonNameResult {
        val given = normalize(givenName)
        val family = normalize(familyName)
        val givenError = fieldError(given)
        var familyError = fieldError(family)

        if (givenError == null && familyError == null) {
            familyError = fullNameError(given, family)
        }

        return if (givenError == null && familyError == null) {
            PersonNameResult.Valid(given, family)
        } else {
            PersonNameResult.Invalid(givenError, familyError)
        }
    }

    private fun fieldError(field: String): PersonNameError? {
        val key = field.toKey()
        val words = field.split(' ').filter { it.isNotEmpty() }
        val letters = field.count { it.isLetter() }
        return when {
            field.isEmpty() -> PersonNameError.EMPTY
            field.looksLikeContact() -> PersonNameError.CONTACT_INFO
            words.any { !WORD.matches(it) } -> PersonNameError.INVALID_CHARACTERS
            letters < MIN_LETTERS -> PersonNameError.TOO_SHORT
            letters > MAX_LETTERS || words.size > MAX_WORDS -> PersonNameError.TOO_LONG
            REPEATED_LETTER.containsMatchIn(key) ||
                key.isKeyboardRun(MIN_KEYBOARD_RUN_FIELD) ||
                key in FIELD_PLACEHOLDERS ||
                key.split(' ').any { it in PROFANITY } -> PersonNameError.NOT_A_NAME
            else -> null
        }
    }

    private fun fullNameError(given: String, family: String): PersonNameError? {
        val full = "$given $family"
        val key = full.toKey()
        return when {
            full.length > MAX_FULL_NAME_LENGTH -> PersonNameError.FULL_NAME_TOO_LONG
            key in FULL_NAME_PLACEHOLDERS || key.isKeyboardRun(MIN_KEYBOARD_RUN_FULL_NAME) ->
                PersonNameError.NOT_A_NAME
            else -> null
        }
    }

    private fun normalize(raw: String): String =
        Normalizer.normalize(raw, Normalizer.Form.NFC)
            .replace('’', '\'')
            .trim()
            .split(WHITESPACE)
            .filter { it.isNotEmpty() }
            .mapIndexed { index, word ->
                if (index > 0 && word.lowercase(Locale.ROOT) in PARTICLES) {
                    word.lowercase(Locale.ROOT)
                } else {
                    word.split('-').joinToString("-") { it.capitalizePart() }
                }
            }
            .joinToString(" ")

    /** "maRIA"/"MARIA" -> "Maria", but a deliberate internal capital ("McDonald", "O'Neil") is kept. */
    private fun String.capitalizePart(): String {
        if (isEmpty()) return this
        val hasDeliberateInternalCapital = first().isUpperCase() &&
            drop(1).any { it.isUpperCase() } &&
            drop(1).any { it.isLowerCase() }
        if (hasDeliberateInternalCapital) return this
        val lower = lowercase(Locale.ROOT)
        return lower.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }

    private fun String.looksLikeContact(): Boolean {
        val lower = lowercase(Locale.ROOT)
        return '@' in lower ||
            "http" in lower ||
            "www." in lower ||
            DOMAIN.containsMatchIn(lower) ||
            count { it.isDigit() } >= MIN_PHONE_DIGITS
    }

    /** Lowercase, accent-free comparison key; keeps spaces between words. */
    private fun String.toKey(): String =
        Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")

    private fun String.isKeyboardRun(minLength: Int): Boolean {
        val compact = filter { it.isLetter() }
        return compact.length >= minLength && KEYBOARD_ROWS.any { compact in it }
    }

    private val WHITESPACE = Regex("\\s+")
    private val COMBINING_MARKS = Regex("\\p{M}+")

    /** Letters (any script) with single inner hyphens/apostrophes: "Ana-Clara", "O'Neil", not "-Ana" or "Ana--Clara". */
    private val WORD = Regex("^[\\p{L}\\p{M}]+(?:['-][\\p{L}\\p{M}]+)*$")
    private val REPEATED_LETTER = Regex("(\\p{L})\\1\\1")
    private val DOMAIN = Regex("[\\p{L}\\d]\\.[a-z]{2,}")

    private val KEYBOARD_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm", "abcdefgh")
        .flatMap { listOf(it, it.reversed()) }

    private val PARTICLES = setOf(
        "da", "de", "do", "dos", "das", "e", "van", "von", "di", "del", "la", "le", "bin",
    )

    private val FIELD_PLACEHOLDERS = setOf(
        "teste", "test", "testing", "usuario", "user", "nome", "sobrenome", "name", "surname",
        "fulano", "beltrano", "ciclano", "tal", "admin", "administrador", "anonimo", "anonymous",
        "null", "none", "xxx", "asdf", "abc", "nombre", "apellido", "prueba", "ejemplo",
    )

    private val FULL_NAME_PLACEHOLDERS = setOf(
        "fulano de tal", "fulano da silva", "john doe", "jane doe", "nome sobrenome", "first last",
        "juan perez", "sem nome", "no name", "usuario teste", "test user",
    )

    /**
     * Whole-word, accent-free match only (never substring, to avoid the Scunthorpe problem).
     * Deliberately excludes words that are also common surnames (e.g. "Dick", "Cock").
     * ponytail: short hard-coded list pending review by a native speaker of each language;
     * move to a remotely updatable list if false positives/negatives show up in the beta.
     */
    private val PROFANITY = setOf(
        // pt-BR
        "merda", "porra", "caralho", "puta", "puto", "buceta", "viado", "arrombado", "foda",
        "fodase", "bosta", "corno",
        // en
        "fuck", "fucker", "shit", "bitch", "cunt", "asshole", "nigger", "faggot", "whore", "slut",
        "pussy",
        // es
        "mierda", "pendejo", "cabron", "gilipollas", "verga", "chingada", "culero", "maricon",
    )
}
