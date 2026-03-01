package com.remnd.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao
) {
    fun getAllReminders(): Flow<List<Reminder>> = dao.getAllReminders()
    fun getActiveReminders(): Flow<List<Reminder>> = dao.getActiveReminders()
    fun getCompletedReminders(): Flow<List<Reminder>> = dao.getCompletedReminders()

    suspend fun getRemindersWithAlarms(): List<Reminder> = dao.getRemindersWithAlarms()
    suspend fun getReminderById(id: Long): Reminder? = dao.getReminderById(id)

    suspend fun addReminder(reminder: Reminder): Long = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = dao.deleteReminder(reminder)
    suspend fun deleteReminderById(id: Long) = dao.deleteReminderById(id)
    suspend fun setCompleted(id: Long, completed: Boolean) = dao.setCompleted(id, completed)
    suspend fun deleteAllCompleted() = dao.deleteAllCompleted()
}
