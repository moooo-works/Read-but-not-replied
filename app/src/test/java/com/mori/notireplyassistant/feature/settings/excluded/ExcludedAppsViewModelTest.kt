package com.mori.notireplyassistant.feature.settings.excluded

import com.mori.notireplyassistant.core.domain.model.InstalledAppUiModel
import com.mori.notireplyassistant.core.repository.InstalledAppsRepository
import com.mori.notireplyassistant.core.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ExcludedAppsViewModelTest {

    @Mock private lateinit var installedAppsRepository: InstalledAppsRepository
    @Mock private lateinit var settingsRepository: SettingsRepository

    private lateinit var viewModel: ExcludedAppsViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val mockExcludedFlow = MutableStateFlow<Set<String>>(emptySet())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val apps = listOf(
            InstalledAppUiModel("App A", "com.app.a", null),
            InstalledAppUiModel("App B", "com.app.b", null)
        )
        `when`(installedAppsRepository.getInstalledApps()).thenReturn(apps)
        `when`(settingsRepository.excludedPackagesFlow).thenReturn(mockExcludedFlow)

        viewModel = ExcludedAppsViewModel(installedAppsRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsAppsAndExclusionState() = runTest(testDispatcher) {
        mockExcludedFlow.value = setOf("com.app.a")

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.size)

        val appA = state.find { it.appInfo.packageName == "com.app.a" }
        assertTrue(appA!!.isExcluded)

        val appB = state.find { it.appInfo.packageName == "com.app.b" }
        assertFalse(appB!!.isExcluded)

        job.cancel()
    }

    @Test
    fun searchFiltersList() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("App B")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.size)
        assertEquals("com.app.b", state[0].appInfo.packageName)

        job.cancel()
    }

    @Test
    fun searchFiltersList_byPackageName() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("com.app.a")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.size)
        assertEquals("com.app.a", state[0].appInfo.packageName)

        job.cancel()
    }

    @Test
    fun toggleExclude_callsRepository() = runTest(testDispatcher) {
        viewModel.onToggleExclude("com.app.a", true)
        advanceUntilIdle()

        verify(settingsRepository).setExcluded("com.app.a", true)
    }
}
