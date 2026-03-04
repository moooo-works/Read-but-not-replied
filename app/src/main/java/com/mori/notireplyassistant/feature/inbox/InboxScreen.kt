@file:OptIn(ExperimentalMaterial3Api::class)

package com.mori.notireplyassistant.feature.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.notireplyassistant.core.domain.model.ConversationUiModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun InboxScreen(
    onNavigateToConversation: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NotiReply Inbox") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Active") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Archived") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Reminders") }
                )
            }

            when (val state = uiState) {
                is InboxUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is InboxUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No conversations")
                    }
                }
                is InboxUiState.Success -> {
                    LazyColumn {
                        items(
                            items = state.conversations,
                            key = { it.conversationId }
                        ) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    // URL Encode ID because it contains pipes |
                                    val encodedId = URLEncoder.encode(conversation.conversationId, StandardCharsets.UTF_8.toString())
                                    onNavigateToConversation(encodedId)
                                },
                                onMarkHandled = { viewModel.onMarkHandled(conversation.conversationId) },
                                onToggleArchive = { viewModel.onArchive(conversation.conversationId, !conversation.isArchived) }
                            )
                            Divider()
                        }
                    }
                }
                is InboxUiState.Reminders -> {
                    RemindersScreen()
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationUiModel,
    onClick: () -> Unit,
    onMarkHandled: () -> Unit,
    onToggleArchive: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.threadType == "GROUP") {
                    Badge(modifier = Modifier.padding(start = 4.dp), containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("Group", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                } else if (conversation.threadType == "DIRECT") {
                    Badge(modifier = Modifier.padding(start = 4.dp), containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text("Direct", color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }

                if (conversation.pendingCount > 0) {
                    Badge(modifier = Modifier.padding(start = 8.dp)) {
                        Text(conversation.pendingCount.toString())
                    }
                }
            }
        },
        supportingContent = {
            Text(
                text = conversation.preview,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = conversation.packageName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onMarkHandled) {
                    Icon(Icons.Default.Check, contentDescription = "Mark Handled")
                }
                IconButton(onClick = onToggleArchive) {
                    Icon(
                        if (conversation.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "Archive"
                    )
                }
            }
        }
    )
}
