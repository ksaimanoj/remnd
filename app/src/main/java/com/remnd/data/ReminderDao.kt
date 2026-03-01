package com.remnd.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    /** Observe all reminders ordered by due time (nulls last), then creation time */
    @Query("""
        SELECT * FROM reminders
        ORDER BY isCompleted ASC,
                 CASE WHEN dueTimeMillis IS NULL THEN 1 ELSE 0 END ASC,
                 dueTimeMillis ASC,
                 createdAtMillis DESC
    """)
    fun getAllReminders(): Flow<List<Reminder>>

    /** Observe only incomplete reminders */
    @Query("""
        SELECT * FROM reminders
        WHERE isCompleted = 0
        ORDER BY CASE WHEN dueTimeMillis IS NULL THEN 1 ELSE 0 END ASC,
                 dueTimeMillis ASC,
                 priority DESC,
                 createdAtMillis DESC
    """)
    fun getActiveReminders(): Flow<List<Reminder>>

    /** Observe only completed reminders */
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY createdAtMillis DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>

    /** Get all reminders that have a due time (for scheduling alarms) */
    @Query("SELECT * FROM reminders WHERE dueTimeMillis IS NOT NULL AND isCompleted = 0")
    suspend fun getRemindersWithAlarms(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun deleteAllCompleted()
}
