package com.mori.notireplyassistant.feature.settings.excluded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.notireplyassistant.core.domain.model.AppInfo
import com.mori.notireplyassistant.core.domain.provider.AppInfoProvider
import com.mori.notireplyassistant.core.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExcludedAppItem(
    val appInfo: AppInfo,
    val isExcluded: Boolean
)

@HiltViewModel
class ExcludedAppsViewModel @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val uiState: StateFlow<List<ExcludedAppItem>> = combine(
        installedApps,
        settingsRepository.excludedPackagesFlow,
        _searchQuery
    ) { apps, excludedSet, query ->
        apps.filter { app ->
            query.isBlank() || app.name.contains(query, ignoreCase = true)
        }.map { app ->
            ExcludedAppItem(app, excludedSet.contains(app.packageName))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            installedApps.value = appInfoProvider.getInstalledApps()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onToggleExclude(packageName: String, exclude: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExcluded(packageName, exclude)
        }
    }
}
