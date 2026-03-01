package com.remnd.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.remnd.ui.components.ReminderItem
import com.remnd.viewmodel.FilterMode
import com.remnd.viewmodel.ReminderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteCompletedDialog by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("remnd") },
                actions = {
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    if (uiState.filterMode == FilterMode.COMPLETED) {
                        IconButton(onClick = { showDeleteCompletedDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear completed")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReminder) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        },
        bottomBar = {
            NavigationBar {
                FilterMode.entries.forEach { mode ->
                    NavigationBarItem(
                        selected = uiState.filterMode == mode,
                        onClick = { viewModel.setFilter(mode) },
                        icon = {
                            Icon(
                                imageVector = when (mode) {
                                    FilterMode.ALL -> Icons.Default.List
                                    FilterMode.ACTIVE -> Icons.Default.RadioButtonUnchecked
                                    FilterMode.COMPLETED -> Icons.Default.CheckCircle
                                },
                                contentDescription = mode.name
                            )
                        },
                        label = {
                            Text(
                                text = when (mode) {
                                    FilterMode.ALL -> "All"
                                    FilterMode.ACTIVE -> "Active"
                                    FilterMode.COMPLETED -> "Done"
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            AnimatedVisibility(visible = searchActive) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search reminders…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.reminders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (uiState.filterMode == FilterMode.COMPLETED)
                                Icons.Default.CheckCircle else Icons.Default.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = when (uiState.filterMode) {
                                FilterMode.ACTIVE -> "No active reminders.\nTap + to add one!"
                                FilterMode.COMPLETED -> "Nothing completed yet."
                                FilterMode.ALL -> "No reminders yet.\nTap + to get started!"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.reminders,
                        key = { it.id }
                    ) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            onToggle = { viewModel.toggleCompleted(reminder) },
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCompletedDialog = false },
            title = { Text("Clear completed") },
            text = { Text("Delete all completed reminders? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllCompleted()
                    showDeleteCompletedDialog = false
                }) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompletedDialog = false }) { Text("Cancel") }
            }
        )
    }
}
