package com.remnd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.remnd.data.FrequencyType
import com.remnd.data.Priority
import com.remnd.data.Reminder
import com.remnd.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    val isEditing = reminderId != null
    val uiState by viewModel.uiState.collectAsState()

    // Load existing reminder when editing
    var existingReminder by remember { mutableStateOf<Reminder?>(null) }
    LaunchedEffect(reminderId, uiState.reminders) {
        if (reminderId != null) {
            existingReminder = uiState.reminders.find { it.id == reminderId }
        }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(Priority.MEDIUM) }
    var dueTimeMillis by remember { mutableStateOf<Long?>(null) }
    var frequencyType by remember { mutableIntStateOf(FrequencyType.NONE) }

    // Populate fields once existing reminder is loaded
    LaunchedEffect(existingReminder) {
        existingReminder?.let { r ->
            title = r.title
            description = r.description
            priority = r.priority
            dueTimeMillis = r.dueTimeMillis
            frequencyType = r.frequencyType
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    var titleError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueTimeMillis ?: System.currentTimeMillis()
    )

    // Derive initial hour/minute from existing due time when editing
    val existingCal = remember(dueTimeMillis) {
        dueTimeMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = existingCal?.get(Calendar.HOUR_OF_DAY) ?: 9,
        initialMinute = existingCal?.get(Calendar.MINUTE) ?: 0
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit reminder" else "New reminder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text("Title *") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Title, null) }
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                leadingIcon = { Icon(Icons.Default.Notes, null) }
            )

            // Priority
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Priority.LOW to "Low", Priority.MEDIUM to "Medium", Priority.HIGH to "High")
                    .forEach { (value, label) ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(label) },
                            leadingIcon = if (priority == value) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
            }

            // Due date/time
            Text("Due date (optional)", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dueTimeMillis?.let {
                            SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date(it))
                        } ?: "Set due date & time"
                    )
                }
                if (dueTimeMillis != null) {
                    IconButton(onClick = {
                        dueTimeMillis = null
                        frequencyType = FrequencyType.NONE
                    }) {
                        Icon(Icons.Default.Clear, "Clear date", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Frequency chips — only visible when a due time is set
            if (dueTimeMillis != null) {
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(FrequencyType.NONE to "Once", FrequencyType.DAILY to "Daily")
                        .forEach { (value, label) ->
                            FilterChip(
                                selected = frequencyType == value,
                                onClick = { frequencyType = value },
                                label = { Text(label) },
                                leadingIcon = if (frequencyType == value) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val effectiveFrequencyType = if (dueTimeMillis == null) FrequencyType.NONE else frequencyType
                    if (isEditing && existingReminder != null) {
                        viewModel.updateReminder(
                            existingReminder!!.copy(
                                title = title.trim(),
                                description = description.trim(),
                                priority = priority,
                                dueTimeMillis = dueTimeMillis,
                                frequencyType = effectiveFrequencyType
                            )
                        )
                    } else {
                        viewModel.addReminder(
                            title = title,
                            description = description,
                            dueTimeMillis = dueTimeMillis,
                            priority = priority,
                            frequencyType = effectiveFrequencyType
                        )
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Save changes" else "Add reminder")
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        pendingDateMillis = millis
                        showDatePicker = false
                        showTimePicker = true
                    } ?: run { showDatePicker = false }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis?.let { dateMillis ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        dueTimeMillis = cal.timeInMillis
                    }
                    showTimePicker = false
                    pendingDateMillis = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    pendingDateMillis = null
                }) { Text("Cancel") }
            }
        )
    }
}
