package com.mori.notireplyassistant.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.notireplyassistant.core.common.NotificationAccessChecker
import com.mori.notireplyassistant.core.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val notificationAccessChecker: NotificationAccessChecker
) : ViewModel() {

    private val _isNotificationAccessEnabled = MutableStateFlow(false)
    val isNotificationAccessEnabled: StateFlow<Boolean> = _isNotificationAccessEnabled.asStateFlow()

    init {
        checkNotificationAccess()
    }

    fun checkNotificationAccess() {
        _isNotificationAccessEnabled.value = notificationAccessChecker.hasAccess()
    }

    fun clearData() {
        viewModelScope.launch {
            repository.clearLocalData(preserveTemplates = true)
        }
    }
}
