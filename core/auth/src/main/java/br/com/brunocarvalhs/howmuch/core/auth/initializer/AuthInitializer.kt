package br.com.brunocarvalhs.howmuch.core.auth.initializer

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.common.initializer.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth

class AuthInitializer : Initializer<FirebaseAuth> {
    override fun create(context: Context): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(FirebaseInitializer::class.java)
}
