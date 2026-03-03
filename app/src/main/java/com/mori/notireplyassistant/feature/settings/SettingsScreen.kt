package com.mori.notireplyassistant.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExcludedApps: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                modifier = Modifier.clickable { onNavigateToExcludedApps() },
                headlineContent = { Text("Excluded Apps") },
                supportingContent = { Text("Manage apps to ignore notifications from") },
                leadingContent = { Icon(Icons.Default.Block, contentDescription = null) }
            )
            Divider()
            ListItem(
                modifier = Modifier.clickable {
                    viewModel.clearData()
                    onNavigateBack() // Navigate back to refresh/show empty inbox
                },
                headlineContent = { Text("Clear All Data") },
                supportingContent = { Text("Delete all messages and conversations") },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }
}
