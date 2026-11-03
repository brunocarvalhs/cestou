package br.com.brunocarvalhs.howmuch.analytics

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk

/** [Context] cujo `getSharedPreferences` devolve um mapa em memória (leitura e escrita reais). */
internal fun contextWithPrefs(store: MutableMap<String, Any> = mutableMapOf()): Context {
    val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    every { editor.putBoolean(any(), any()) } answers {
        store[firstArg()] = secondArg<Boolean>()
        editor
    }
    every { editor.putString(any(), any()) } answers {
        store[firstArg()] = secondArg<String>()
        editor
    }
    every { editor.putLong(any(), any()) } answers {
        store[firstArg()] = secondArg<Long>()
        editor
    }
    val prefs = mockk<SharedPreferences>()
    every { prefs.edit() } returns editor
    every { prefs.getBoolean(any(), any()) } answers { store[firstArg()] as? Boolean ?: secondArg() }
    every { prefs.getString(any(), any()) } answers { store[firstArg()] as? String ?: secondArg() }
    every { prefs.getLong(any(), any()) } answers { store[firstArg()] as? Long ?: secondArg() }
    return mockk { every { getSharedPreferences(any(), any()) } returns prefs }
}
