package br.com.brunocarvalhs.data.extensions

import java.security.SecureRandom

val secureRandom = SecureRandom()

private const val NEXT_INT = 900_000
private const val NUMBER_VALUE = 100_000

fun randomToken(): String {
    val number = secureRandom.nextInt(NEXT_INT) + NUMBER_VALUE
    return number.toString()
}
