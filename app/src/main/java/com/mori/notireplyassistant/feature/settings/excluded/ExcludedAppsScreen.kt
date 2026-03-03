package com.mori.notireplyassistant.feature.settings.excluded

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExcludedAppsViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val apps by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excluded Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = apps,
                    key = { it.appInfo.packageName }
                ) { item ->
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.onToggleExclude(item.appInfo.packageName, !item.isExcluded)
                        },
                        headlineContent = { Text(item.appInfo.name) },
                        supportingContent = { Text(item.appInfo.packageName) },
                        leadingContent = {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.appInfo.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = item.isExcluded,
                                onCheckedChange = {
                                    viewModel.onToggleExclude(item.appInfo.packageName, it)
                                }
                            )
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
