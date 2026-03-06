package com.mori.notireplyassistant.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.notireplyassistant.core.domain.model.ReminderUiModel
import com.mori.notireplyassistant.core.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    val reminders: StateFlow<List<ReminderUiModel>> = repository.observeActiveReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onDismiss(reminderId: Long) {
        viewModelScope.launch {
            repository.dismissReminder(reminderId)
        }
    }

    fun onSnooze(reminderId: Long) {
        viewModelScope.launch {
            repository.snoozeReminder(reminderId)
        }
    }
}
