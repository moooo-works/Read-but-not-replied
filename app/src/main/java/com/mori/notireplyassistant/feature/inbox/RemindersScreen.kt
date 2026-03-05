package com.mori.notireplyassistant.feature.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.notireplyassistant.core.database.entity.ReminderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending reminders")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = reminders,
                key = { it.reminderId }
            ) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    onDismiss = { viewModel.onDismiss(reminder.reminderId) },
                    onSnooze = { viewModel.onSnooze(reminder.reminderId) }
                )
                Divider()
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: ReminderEntity,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val timeString = formatter.format(Date(reminder.scheduledTime))

    ListItem(
        headlineContent = {
            Text(
                text = "Reminder for conversation",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                if (!reminder.note.isNullOrBlank()) {
                    Text(text = "Note: ${reminder.note}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(text = "Scheduled: $timeString", color = MaterialTheme.colorScheme.primary)
            }
        },
        leadingContent = {
            Icon(Icons.Default.Notifications, contentDescription = null)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onSnooze) {
                    Icon(Icons.Default.Snooze, contentDescription = "Snooze 10m")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
        }
    )
}
