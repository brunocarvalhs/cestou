package br.com.brunocarvalhs.howmuch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class MainAppTest : ComposeTest() {

    private val activity by lazy { Robolectric.buildActivity(MainActivity::class.java) }

    @Test
    fun mainApp_rendersWithoutCrash() {
        composeTestRule.setContent {
            MainApp(
                navController = rememberNavController()
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
