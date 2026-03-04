package com.mori.notireplyassistant.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.notireplyassistant.core.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    fun clearData() {
        viewModelScope.launch {
            repository.clearLocalData(preserveTemplates = true)
        }
    }
}
