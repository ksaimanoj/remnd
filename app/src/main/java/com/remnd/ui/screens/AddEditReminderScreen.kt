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
    var dueDateError by remember { mutableStateOf(false) }

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

    // Whether the current frequency requires date+time, time only, or nothing
    val needsDateTime = frequencyType == FrequencyType.NONE
    val needsTimeOnly = frequencyType == FrequencyType.DAILY

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

            // Repeat — always visible
            Text("Repeat", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(FrequencyType.NONE to "Once", FrequencyType.DAILY to "Daily", FrequencyType.HOURLY to "Hourly")
                    .forEach { (value, label) ->
                        FilterChip(
                            selected = frequencyType == value,
                            onClick = {
                                frequencyType = value
                                // Hourly needs no date/time — clear it
                                if (value == FrequencyType.HOURLY) {
                                    dueTimeMillis = null
                                    dueDateError = false
                                }
                            },
                            label = { Text(label) },
                            leadingIcon = if (frequencyType == value) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
            }

            // Due date and time — hidden for Hourly
            if (needsDateTime || needsTimeOnly) {
                val sectionLabel = if (needsTimeOnly) "Time *" else "Due date and time *"
                val placeholder = if (needsTimeOnly) "Set time" else "Set due date & time"
                val dateFormat = if (needsTimeOnly) "h:mm a" else "MMM d, yyyy  h:mm a"

                Text(
                    sectionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (dueDateError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            dueDateError = false
                            if (needsTimeOnly) showTimePicker = true
                            else showDatePicker = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (dueDateError) ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(
                            if (needsTimeOnly) Icons.Default.Schedule else Icons.Default.CalendarToday,
                            null,
                            Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            dueTimeMillis?.let {
                                SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(it))
                            } ?: placeholder
                        )
                    }
                    if (dueTimeMillis != null) {
                        IconButton(onClick = {
                            dueTimeMillis = null
                            dueDateError = false
                        }) {
                            Icon(Icons.Default.Clear, "Clear", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (dueDateError) {
                    Text(
                        if (needsTimeOnly) "Time is required" else "Due date and time is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    var hasError = false
                    if (title.isBlank()) {
                        titleError = true
                        hasError = true
                    }
                    if ((needsDateTime || needsTimeOnly) && dueTimeMillis == null) {
                        dueDateError = true
                        hasError = true
                    }
                    if (hasError) return@Button

                    val effectiveFrequencyType = frequencyType
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

    // Date picker dialog (Once only)
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
            onDismissRequest = {
                showTimePicker = false
                pendingDateMillis = null
            },
            title = { Text("Select time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    // For Daily, use today's date as the base; for Once, use the picked date
                    val baseDateMillis = pendingDateMillis ?: Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = baseDateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    dueTimeMillis = cal.timeInMillis
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
