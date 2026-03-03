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
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlinx.coroutines.launch

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
    fun isExcluded_beforeReady_returnsFalse() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)

        // At this point, the initialValue is emptySet, but the flow hasn't emitted from dataStore yet.
        // So isReady should be false, and isExcluded should return false.
        assertFalse(repository.isExcluded("com.example.app"))
    }

    @Test
    fun defaultExcludedPackages_isEmpty() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)

        // Force flow collection so it reads from datastore
        val job = backgroundScope.launch {
            repository.excludedPackagesFlow.collect {}
        }
        testScheduler.advanceUntilIdle()

        // Write something and read to ensure it's fully ready
        repository.setExcluded("com.dummy", true)
        testScheduler.advanceUntilIdle()
        repository.setExcluded("com.dummy", false)
        testScheduler.advanceUntilIdle()

        val current = repository.excludedPackagesFlow.value
        assertTrue(current.isEmpty())
        assertFalse(repository.isExcluded("com.example.app"))
        job.cancel()
    }

    @Test
    fun addExcludedPackage_updatesFlowAndCache() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)
        val job = backgroundScope.launch { repository.excludedPackagesFlow.collect {} }

        testScheduler.advanceUntilIdle() // Ensure it is ready before we check

        repository.setExcluded("com.example.app", true)
        testScheduler.advanceUntilIdle() // Wait for flow updates

        val flowValue = repository.excludedPackagesFlow.value
        assertTrue(flowValue.contains("com.example.app"))
        assertTrue(repository.isExcluded("com.example.app"))
        job.cancel()
    }

    @Test
    fun removeExcludedPackage_updatesFlowAndCache() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)
        val job = backgroundScope.launch { repository.excludedPackagesFlow.collect {} }

        testScheduler.advanceUntilIdle()

        repository.setExcluded("com.example.app", true)
        testScheduler.advanceUntilIdle()

        repository.setExcluded("com.example.app", false)
        testScheduler.advanceUntilIdle()

        val flowValue = repository.excludedPackagesFlow.value
        assertFalse(flowValue.contains("com.example.app"))
        assertFalse(repository.isExcluded("com.example.app"))
        job.cancel()
    }

    @Test
    fun clearAllExcluded_clearsSet() = runTest(testDispatcher) {
        val repository = SettingsRepository(settingsDataStore, externalScope = backgroundScope)
        val job = backgroundScope.launch { repository.excludedPackagesFlow.collect {} }

        testScheduler.advanceUntilIdle()

        repository.setExcluded("com.app1", true)
        repository.setExcluded("com.app2", true)
        testScheduler.advanceUntilIdle()

        repository.clearAllExcluded()
        testScheduler.advanceUntilIdle()

        assertTrue(repository.excludedPackagesFlow.value.isEmpty())
        job.cancel()
    }
}
