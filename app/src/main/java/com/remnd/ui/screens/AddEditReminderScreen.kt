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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.remnd.data.FrequencyType
import com.remnd.data.Priority
import com.remnd.data.Reminder
import com.remnd.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun nextDailyOccurrence(ref: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ref }
    val now = Calendar.getInstance()
    while (!cal.after(now)) cal.add(Calendar.DAY_OF_YEAR, 1)
    return cal.timeInMillis
}

private fun nextHourlyOccurrence(ref: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ref }
    val now = Calendar.getInstance()
    while (!cal.after(now)) cal.add(Calendar.HOUR_OF_DAY, 1)
    return cal.timeInMillis
}

private fun upcomingOccurrences(dueTimeMillis: Long, frequencyType: Int, count: Int = 3): List<Long> {
    val result = mutableListOf<Long>()
    var ref = dueTimeMillis
    repeat(count) {
        val next = if (frequencyType == FrequencyType.HOURLY) nextHourlyOccurrence(ref)
                   else nextDailyOccurrence(ref)
        result.add(next)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        if (frequencyType == FrequencyType.HOURLY) cal.add(Calendar.HOUR_OF_DAY, 1)
        else cal.add(Calendar.DAY_OF_YEAR, 1)
        ref = cal.timeInMillis
    }
    return result
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    val isEditing = reminderId != null
    val uiState by viewModel.uiState.collectAsState()

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

    LaunchedEffect(existingReminder) {
        existingReminder?.let { r ->
            title = r.title
            description = r.description
            priority = r.priority
            dueTimeMillis = r.dueTimeMillis
            frequencyType = r.frequencyType
        }
    }

    // Dialog visibility
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMinutePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    // Validation errors
    var titleError by remember { mutableStateOf(false) }
    var dueDateError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueTimeMillis ?: System.currentTimeMillis()
    )
    val existingCal = remember(dueTimeMillis) {
        dueTimeMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = existingCal?.get(Calendar.HOUR_OF_DAY) ?: 9,
        initialMinute = existingCal?.get(Calendar.MINUTE) ?: 0
    )

    // Minute picker state (for Hourly)
    var selectedMinute by remember { mutableIntStateOf(0) }

    // Upcoming occurrences for Daily / Hourly
    val upcomingTimes by remember(dueTimeMillis, frequencyType) {
        derivedStateOf {
            if (dueTimeMillis != null &&
                (frequencyType == FrequencyType.DAILY || frequencyType == FrequencyType.HOURLY)
            ) {
                upcomingOccurrences(dueTimeMillis!!, frequencyType)
            } else emptyList()
        }
    }

    val upcomingFmt = remember { SimpleDateFormat("EEE, MMM d  h:mm a", Locale.getDefault()) }

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
                onValueChange = { title = it; titleError = false },
                label = { Text("Title *") },
                isError = titleError,
                supportingText = if (titleError) { { Text("Title is required") } } else null,
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

            // ── Repeat ────────────────────────────────────────────────────
            Text("Repeat", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    FrequencyType.NONE to "Once",
                    FrequencyType.DAILY to "Daily",
                    FrequencyType.HOURLY to "Hourly"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = frequencyType == value,
                        onClick = {
                            frequencyType = value
                            // Clear date/time when switching away from Once/Daily
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

            // ── Date / Time / Minute field ─────────────────────────────────
            when (frequencyType) {
                FrequencyType.NONE -> {
                    // Once — full date + time (mandatory)
                    DateTimeField(
                        label = "Due date and time *",
                        placeholder = "Set due date & time",
                        dateFormat = "MMM d, yyyy  h:mm a",
                        icon = Icons.Default.CalendarToday,
                        dueTimeMillis = dueTimeMillis,
                        hasError = dueDateError,
                        errorText = "Due date and time is required",
                        onClear = { dueTimeMillis = null; dueDateError = false },
                        onClick = { dueDateError = false; showDatePicker = true }
                    )
                }
                FrequencyType.DAILY -> {
                    // Daily — time only (mandatory)
                    DateTimeField(
                        label = "Time *",
                        placeholder = "Set time",
                        dateFormat = "h:mm a",
                        icon = Icons.Default.Schedule,
                        dueTimeMillis = dueTimeMillis,
                        hasError = dueDateError,
                        errorText = "Time is required",
                        onClear = { dueTimeMillis = null; dueDateError = false },
                        onClick = { dueDateError = false; showTimePicker = true }
                    )
                }
                FrequencyType.HOURLY -> {
                    // Hourly — minute only (mandatory)
                    val minuteLabel = dueTimeMillis?.let {
                        val m = Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE)
                        ":${m.toString().padStart(2, '0')} of every hour"
                    } ?: "Select minute"
                    val labelColor = if (dueDateError) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.onSurface
                    Text(
                        "At minute *",
                        style = MaterialTheme.typography.labelLarge,
                        color = labelColor
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                dueDateError = false
                                selectedMinute = dueTimeMillis?.let {
                                    Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE)
                                } ?: 0
                                showMinutePicker = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (dueDateError) ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(minuteLabel)
                        }
                        if (dueTimeMillis != null) {
                            IconButton(onClick = { dueTimeMillis = null; dueDateError = false }) {
                                Icon(Icons.Default.Clear, "Clear", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (dueDateError) {
                        Text(
                            "Minute is required",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // ── Upcoming reminders ─────────────────────────────────────────
            if (upcomingTimes.isNotEmpty()) {
                Text(
                    "Upcoming reminders at",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                upcomingTimes.forEach { millis ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            upcomingFmt.format(Date(millis)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Save ───────────────────────────────────────────────────────
            Button(
                onClick = {
                    var hasError = false
                    if (title.isBlank()) { titleError = true; hasError = true }
                    if (dueTimeMillis == null) { dueDateError = true; hasError = true }
                    if (hasError) return@Button

                    if (isEditing && existingReminder != null) {
                        viewModel.updateReminder(
                            existingReminder!!.copy(
                                title = title.trim(),
                                description = description.trim(),
                                priority = priority,
                                dueTimeMillis = dueTimeMillis,
                                frequencyType = frequencyType
                            )
                        )
                    } else {
                        viewModel.addReminder(
                            title = title.trim(),
                            description = description.trim(),
                            dueTimeMillis = dueTimeMillis,
                            priority = priority,
                            frequencyType = frequencyType
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

    // ── Date picker dialog ─────────────────────────────────────────────────
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

    // ── Time picker dialog ─────────────────────────────────────────────────
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false; pendingDateMillis = null },
            title = { Text("Select time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val baseDateMillis = pendingDateMillis ?: Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = baseDateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    dueTimeMillis = cal.timeInMillis
                    showTimePicker = false
                    pendingDateMillis = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false; pendingDateMillis = null }) { Text("Cancel") }
            }
        )
    }

    // ── Minute picker dialog (Hourly) ──────────────────────────────────────
    if (showMinutePicker) {
        AlertDialog(
            onDismissRequest = { showMinutePicker = false },
            title = { Text("Select minute") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Fire at :MM of every hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilledIconButton(
                            onClick = { selectedMinute = (selectedMinute - 1 + 60) % 60 }
                        ) {
                            Icon(Icons.Default.Remove, "Decrease minute")
                        }
                        Text(
                            ":${selectedMinute.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        FilledIconButton(
                            onClick = { selectedMinute = (selectedMinute + 1) % 60 }
                        ) {
                            Icon(Icons.Default.Add, "Increase minute")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Store as today midnight + selected minute (only minute component matters for hourly)
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    dueTimeMillis = cal.timeInMillis
                    showMinutePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showMinutePicker = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Reusable date/time button field ───────────────────────────────────────────

@Composable
private fun DateTimeField(
    label: String,
    placeholder: String,
    dateFormat: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    dueTimeMillis: Long?,
    hasError: Boolean,
    errorText: String,
    onClear: () -> Unit,
    onClick: () -> Unit
) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (hasError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            colors = if (hasError) ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ) else ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                dueTimeMillis?.let {
                    SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(it))
                } ?: placeholder
            )
        }
        if (dueTimeMillis != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, "Clear", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (hasError) {
        Text(
            errorText,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
