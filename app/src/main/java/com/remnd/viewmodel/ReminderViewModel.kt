package com.remnd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remnd.data.FrequencyType
import com.remnd.data.Priority
import com.remnd.data.Reminder
import com.remnd.data.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FilterMode { ALL, ACTIVE, COMPLETED }

data class ReminderUiState(
    val reminders: List<Reminder> = emptyList(),
    val filterMode: FilterMode = FilterMode.ACTIVE,
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    private val _filterMode = MutableStateFlow(FilterMode.ACTIVE)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ReminderUiState> = combine(
        _filterMode,
        _searchQuery
    ) { mode, query -> mode to query }
        .flatMapLatest { (mode, query) ->
            val flow = when (mode) {
                FilterMode.ALL -> repository.getAllReminders()
                FilterMode.ACTIVE -> repository.getActiveReminders()
                FilterMode.COMPLETED -> repository.getCompletedReminders()
            }
            flow.map { list ->
                val filtered = if (query.isBlank()) list
                else list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
                ReminderUiState(
                    reminders = filtered,
                    filterMode = mode,
                    isLoading = false,
                    searchQuery = query
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReminderUiState()
        )

    fun setFilter(mode: FilterMode) {
        _filterMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addReminder(
        title: String,
        description: String = "",
        dueTimeMillis: Long? = null,
        priority: Int = Priority.MEDIUM,
        frequencyType: Int = FrequencyType.NONE
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addReminder(
                Reminder(
                    title = title.trim(),
                    description = description.trim(),
                    dueTimeMillis = dueTimeMillis,
                    priority = priority,
                    frequencyType = frequencyType
                )
            )
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch { repository.updateReminder(reminder) }
    }

    fun toggleCompleted(reminder: Reminder) {
        viewModelScope.launch {
            repository.setCompleted(reminder.id, !reminder.isCompleted)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch { repository.deleteReminder(reminder) }
    }

    fun deleteAllCompleted() {
        viewModelScope.launch { repository.deleteAllCompleted() }
    }
}
