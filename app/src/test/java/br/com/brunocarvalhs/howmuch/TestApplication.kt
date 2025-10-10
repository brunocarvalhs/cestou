package br.com.brunocarvalhs.howmuch

import android.app.Application
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.TestAnalytics
import com.google.firebase.FirebaseApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
class TestApplication : Application() {
    private val firebaseApp = mockk<FirebaseApp>(relaxed = true)

    override fun onCreate() {
        super.onCreate()
        mockFirebase()
        TestAnalytics.setupMocks()
    }

    private fun mockFirebase() {
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.getInstance() } returns firebaseApp
        every { FirebaseApp.initializeApp(any()) } returns firebaseApp
    }
}
