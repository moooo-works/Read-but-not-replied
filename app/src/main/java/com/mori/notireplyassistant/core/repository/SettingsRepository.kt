package com.mori.notireplyassistant.core.repository

import com.mori.notireplyassistant.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    externalScope: CoroutineScope
) {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // Hot flow, kept active to cache the exclusion list
    val excludedPackagesFlow: StateFlow<Set<String>> = settingsDataStore.excludedPackagesFlow
        .onEach { _isReady.value = true }
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    fun isExcluded(packageName: String): Boolean {
        if (!_isReady.value) {
            // Conservative ignore before DataStore is fully loaded
            return true
        }
        return excludedPackagesFlow.value.contains(packageName)
    }

    suspend fun setExcluded(packageName: String, excluded: Boolean) {
        settingsDataStore.setExcluded(packageName, excluded)
    }

    suspend fun clearAllExcluded() {
        settingsDataStore.clearAllExcluded()
    }
}
