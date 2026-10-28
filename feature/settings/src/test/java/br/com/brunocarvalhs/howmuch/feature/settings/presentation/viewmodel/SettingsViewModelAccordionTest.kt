package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.contract.AppVersionProvider
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.UpdateLanguageUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateCurrencyUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Separada de SettingsViewModelTest para não ultrapassar o limite de funções por classe do
// Detekt (TooManyFunctions) — cobre especificamente o accordion não-exclusivo (sdd-011).
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelAccordionTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val updateLanguageUseCase = mockk<UpdateLanguageUseCase>(relaxed = true)
    private val updateCurrencyUseCase = mockk<UpdateCurrencyUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val appVersionProvider = mockk<AppVersionProvider>(relaxed = true)

    private fun viewModel(): SettingsViewModel {
        every { settingsRepository.getSettings() } returns flowOf(AppSettings())
        return SettingsViewModel(
            context,
            settingsRepository,
            updateLanguageUseCase,
            updateCurrencyUseCase,
            analyticsTracker,
            appVersionProvider
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `buildSections assigns stable and unique ids to every section`() {
        val vm = viewModel()

        val ids = vm.uiState.value.sections.map { it.id }

        assertEquals(
            listOf(
                SECTION_ID_GENERAL,
                SECTION_ID_AI,
                SECTION_ID_SHOPPING,
                SECTION_ID_DATA,
                SECTION_ID_NOTIFICATIONS,
                SECTION_ID_SUPPORT,
                SECTION_ID_ABOUT
            ),
            ids
        )
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `onToggleSection expands a collapsed section`() {
        val vm = viewModel()

        vm.intent.onToggleSection(SECTION_ID_GENERAL)

        assertEquals(setOf(SECTION_ID_GENERAL), vm.uiState.value.expandedSectionIds)
    }

    @Test
    fun `onToggleSection collapses an already expanded section`() {
        val vm = viewModel()
        vm.intent.onToggleSection(SECTION_ID_GENERAL)

        vm.intent.onToggleSection(SECTION_ID_GENERAL)

        assertEquals(emptySet<String>(), vm.uiState.value.expandedSectionIds)
    }

    @Test
    fun `onToggleSection does not collapse other already expanded sections`() {
        val vm = viewModel()
        vm.intent.onToggleSection(SECTION_ID_GENERAL)

        vm.intent.onToggleSection(SECTION_ID_AI)

        assertEquals(setOf(SECTION_ID_GENERAL, SECTION_ID_AI), vm.uiState.value.expandedSectionIds)
    }

    @Test
    fun `onToggleSection tracks the toggle event with the section id and the new expanded state`() {
        val vm = viewModel()

        vm.intent.onToggleSection(SECTION_ID_GENERAL)

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.SETTINGS_SECTION_TOGGLED,
                mapOf("section_id" to SECTION_ID_GENERAL, "expanded" to true)
            )
        }
    }
}
