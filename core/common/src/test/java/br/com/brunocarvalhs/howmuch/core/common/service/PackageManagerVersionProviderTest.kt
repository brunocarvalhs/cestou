package br.com.brunocarvalhs.howmuch.core.common.service

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PackageManagerVersionProviderTest {

    private val context = mockk<Context>()
    private val packageManager = mockk<PackageManager>()

    @Before
    fun setup() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "br.com.brunocarvalhs.howmuch"
    }

    private fun stubVersionName(value: String?) {
        val info = PackageInfo().apply { versionName = value }
        every { packageManager.getPackageInfo("br.com.brunocarvalhs.howmuch", 0) } returns info
    }

    @Test
    fun `versionName strips debug build type suffix`() {
        stubVersionName("1.3.0-debug")

        assertEquals("1.3.0", PackageManagerVersionProvider(context).versionName())
    }

    @Test
    fun `versionName returns as-is when there is no suffix`() {
        stubVersionName("1.3.0")

        assertEquals("1.3.0", PackageManagerVersionProvider(context).versionName())
    }

    @Test
    fun `versionName returns empty string when PackageInfo has no versionName`() {
        stubVersionName(null)

        assertEquals("", PackageManagerVersionProvider(context).versionName())
    }
}
