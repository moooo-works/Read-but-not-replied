package com.mori.notireplyassistant.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.mori.notireplyassistant.core.datastore.SettingsDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var settingsDataStore: SettingsDataStore

    // We cannot create repository in @Before because we need backgroundScope from runTest
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        val testFile = tmpFolder.newFile("test_settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)
    }

    @Test
    fun defaultExcludedPackages_isEmpty() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)

        val current = repository.excludedPackagesFlow.value
        assertTrue(current.isEmpty())
        assertFalse(repository.isExcluded("com.example.app"))
    }

    @Test
    fun addExcludedPackage_updatesFlowAndCache() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)

        repository.setExcluded("com.example.app", true)
        testScheduler.advanceUntilIdle() // Wait for flow updates

        val flowValue = repository.excludedPackagesFlow.value
        assertTrue(flowValue.contains("com.example.app"))
        assertTrue(repository.isExcluded("com.example.app"))
    }

    @Test
    fun removeExcludedPackage_updatesFlowAndCache() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)

        repository.setExcluded("com.example.app", true)
        testScheduler.advanceUntilIdle()

        repository.setExcluded("com.example.app", false)
        testScheduler.advanceUntilIdle()

        val flowValue = repository.excludedPackagesFlow.value
        assertFalse(flowValue.contains("com.example.app"))
        assertFalse(repository.isExcluded("com.example.app"))
    }

    @Test
    fun clearAllExcluded_clearsSet() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)

        repository.setExcluded("com.app1", true)
        repository.setExcluded("com.app2", true)
        testScheduler.advanceUntilIdle()

        repository.clearAllExcluded()
        testScheduler.advanceUntilIdle()

        assertTrue(repository.excludedPackagesFlow.value.isEmpty())
    }
}
