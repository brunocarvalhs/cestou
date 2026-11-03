package br.com.brunocarvalhs.howmuch.feature.shopping.data.text

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ResourceNotificationTextsTest {

    private val texts = ResourceNotificationTexts(ApplicationProvider.getApplicationContext())

    @Test
    fun `just now is not blank`() {
        assertTrue(texts.justNow().isNotBlank())
    }

    @Test
    fun `minutes and hours carry their quantity`() {
        assertTrue(texts.minutesAgo(5).contains("5"))
        assertTrue(texts.minutesAgo(1).contains("1"))
        assertTrue(texts.hoursAgo(3).contains("3"))
    }

    @Test
    fun `short date includes the year`() {
        assertTrue(texts.shortDate(LocalDate.of(2026, 10, 3)).contains("2026"))
    }
}
