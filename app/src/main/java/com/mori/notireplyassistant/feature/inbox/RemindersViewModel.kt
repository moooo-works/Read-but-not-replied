package com.mori.notireplyassistant.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.entity.ReminderEntity
import com.mori.notireplyassistant.core.domain.scheduler.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val db: NotiReplyDatabase,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    val reminders: StateFlow<List<ReminderEntity>> = db.reminderDao().getPendingReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onDismiss(reminderId: Long) {
        viewModelScope.launch {
            db.reminderDao().updateStatus(reminderId, "DISMISSED")
            scheduler.cancel(reminderId)
        }
    }

    fun onSnooze(reminderId: Long) {
        viewModelScope.launch {
            val reminder = db.reminderDao().getReminderById(reminderId) ?: return@launch

            // Snooze for 10 minutes
            val snoozeMs = 10 * 60 * 1000L
            val newTime = System.currentTimeMillis() + snoozeMs

            val updated = reminder.copy(
                scheduledTime = newTime,
                status = "SNOOZED"
            )

            // Insert with OnConflictStrategy.REPLACE will update
            db.reminderDao().insertReminder(updated)

            // Schedule the new time
            scheduler.schedule(reminderId, newTime)
        }
    }
}
