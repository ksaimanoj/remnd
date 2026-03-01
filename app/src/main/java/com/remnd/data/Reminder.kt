package com.remnd.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single reminder/to-do item.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Short title for the reminder */
    val title: String,

    /** Optional longer description */
    val description: String = "",

    /** Unix epoch millis for when this reminder should fire (null = no alarm) */
    val dueTimeMillis: Long? = null,

    /** Whether the reminder has been completed/checked off */
    val isCompleted: Boolean = false,

    /** Priority level: 0 = Low, 1 = Medium, 2 = High */
    val priority: Int = Priority.MEDIUM,

    /** Unix epoch millis when this reminder was created */
    val createdAtMillis: Long = System.currentTimeMillis()
)

object Priority {
    const val LOW = 0
    const val MEDIUM = 1
    const val HIGH = 2
}
