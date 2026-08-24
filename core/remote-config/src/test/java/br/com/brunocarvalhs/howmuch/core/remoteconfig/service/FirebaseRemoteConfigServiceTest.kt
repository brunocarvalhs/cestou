package br.com.brunocarvalhs.howmuch.core.remoteconfig.service

import br.com.brunocarvalhs.howmuch.core.common.contract.AppVersionProvider
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.remoteconfig.exception.RemoteConfigException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseRemoteConfigServiceTest {

    private val remoteConfig = mockk<FirebaseRemoteConfig>()
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val versionProvider = mockk<AppVersionProvider>()
    private lateinit var service: FirebaseRemoteConfigService

    @Before
    fun setup() {
        every { versionProvider.versionName() } returns "1.3.0"
        service = FirebaseRemoteConfigService(remoteConfig, crashReporter, versionProvider)
    }

    private fun stubValue(source: Int, asString: String = "", asBoolean: Boolean = false) {
        val value = mockk<FirebaseRemoteConfigValue>()
        every { value.source } returns source
        every { value.asString() } returns asString
        every { value.asBoolean() } returns asBoolean
        every { remoteConfig.getValue("key") } returns value
    }

    @Test
    fun `isEnabled returns default when value is still static`() {
        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_STATIC)

        assertTrue(service.isEnabled("key", default = true))
        assertFalse(service.isEnabled("key", default = false))
    }

    @Test
    fun `isEnabled falls back to plain boolean when value is not JSON`() {
        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asString = "true")
        assertTrue(service.isEnabled("key", default = false))

        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asString = "false")
        assertFalse(service.isEnabled("key", default = true))
    }

    @Test
    fun `isEnabled uses default when value is neither JSON nor a plain boolean`() {
        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asString = "not-a-boolean")

        assertTrue(service.isEnabled("key", default = true))
    }

    @Test
    fun `isEnabled respects enabled flag from JSON config without version bounds`() {
        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asString = """{"enabled":true}""")
        assertTrue(service.isEnabled("key", default = false))

        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asString = """{"enabled":false}""")
        assertFalse(service.isEnabled("key", default = true))
    }

    @Test
    fun `isEnabled is false when current version is in disabledVersions`() {
        stubValue(
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
            asString = """{"enabled":true,"disabledVersions":["1.3.0"]}"""
        )

        assertFalse(service.isEnabled("key", default = true))
    }

    @Test
    fun `isEnabled is true when current version is not in disabledVersions`() {
        stubValue(
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
            asString = """{"enabled":true,"disabledVersions":["1.2.0"]}"""
        )

        assertTrue(service.isEnabled("key", default = false))
    }

    @Test
    fun `isEnabled is false when current version is below minVersion`() {
        stubValue(
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
            asString = """{"enabled":true,"minVersion":"1.4.0"}"""
        )

        assertFalse(service.isEnabled("key", default = true))
    }

    @Test
    fun `isEnabled is true when current version meets minVersion`() {
        stubValue(
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
            asString = """{"enabled":true,"minVersion":"1.3.0"}"""
        )

        assertTrue(service.isEnabled("key", default = false))
    }

    @Test
    fun `isEnabled is false when current version is above maxVersion`() {
        stubValue(
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
            asString = """{"enabled":true,"maxVersion":"1.2.0"}"""
        )

        assertFalse(service.isEnabled("key", default = true))
    }

    @Test
    fun `isEnabled is true when current version meets maxVersion`() {
        stubValue(
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE,
            asString = """{"enabled":true,"maxVersion":"1.3.0"}"""
        )

        assertTrue(service.isEnabled("key", default = false))
    }

    @Test
    fun `isEnabled falls back to default and reports exception on malformed JSON`() {
        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asString = """{"enabled":""")

        assertTrue(service.isEnabled("key", default = true))
        verify { crashReporter.recordException(any<RemoteConfigException>()) }
    }

    @Test
    fun `getString returns default for static source and remote value otherwise`() {
        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_STATIC, asString = "remote")
        assertEquals("default", service.getString("key", "default"))

        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asString = "remote")
        assertEquals("remote", service.getString("key", "default"))
    }

    @Test
    fun `getBoolean returns default for static source and remote value otherwise`() {
        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_STATIC, asBoolean = true)
        assertFalse(service.getBoolean("key", false))

        stubValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asBoolean = true)
        assertTrue(service.getBoolean("key", false))
    }

    @Test
    fun `refresh returns the fetchAndActivate result on success`() = runTest {
        every { remoteConfig.fetchAndActivate() } returns Tasks.forResult(true)

        assertTrue(service.refresh())
    }

    @Test
    fun `refresh returns false and reports exception on failure`() = runTest {
        val error = RuntimeException("network down")
        every { remoteConfig.fetchAndActivate() } returns Tasks.forException(error)

        assertFalse(service.refresh())
        verify { crashReporter.recordException(any<RemoteConfigException>()) }
    }
}
