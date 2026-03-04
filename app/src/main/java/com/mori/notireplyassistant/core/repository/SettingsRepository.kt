package com.mori.notireplyassistant.core.repository

import com.mori.notireplyassistant.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val _isReadyFlow = MutableStateFlow(false)
    val isReadyFlow: StateFlow<Boolean> = _isReadyFlow.asStateFlow()

    val isReady: Boolean
        get() = _isReadyFlow.value

    // Hot flow, kept active to cache the exclusion list
    val excludedPackagesFlow: StateFlow<Set<String>> = settingsDataStore.excludedPackagesFlow
        .onEach { _isReadyFlow.value = true }
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    fun isExcluded(packageName: String): Boolean {
        // If not ready yet, return false (do not exclude)
        if (!isReady) return false
        return excludedPackagesFlow.value.contains(packageName)
    }

    suspend fun setExcluded(packageName: String, excluded: Boolean) {
        settingsDataStore.setExcluded(packageName, excluded)
    }

    suspend fun clearAllExcluded() {
        settingsDataStore.clearAllExcluded()
    }
}
