package com.remnd.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao,
    private val alarmScheduler: AlarmScheduler
) {
    fun getAllReminders(): Flow<List<Reminder>> = dao.getAllReminders()
    fun getActiveReminders(): Flow<List<Reminder>> = dao.getActiveReminders()
    fun getCompletedReminders(): Flow<List<Reminder>> = dao.getCompletedReminders()

    suspend fun getRemindersWithAlarms(): List<Reminder> = dao.getRemindersWithAlarms()
    suspend fun getReminderById(id: Long): Reminder? = dao.getReminderById(id)

    suspend fun addReminder(reminder: Reminder): Long {
        val insertedId = dao.insertReminder(reminder)
        alarmScheduler.schedule(reminder.copy(id = insertedId))
        return insertedId
    }

    suspend fun updateReminder(reminder: Reminder) {
        alarmScheduler.cancel(reminder.id)
        dao.updateReminder(reminder)
        alarmScheduler.schedule(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        alarmScheduler.cancel(reminder.id)
        dao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: Long) {
        alarmScheduler.cancel(id)
        dao.deleteReminderById(id)
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        if (completed) {
            alarmScheduler.cancel(id)
        } else {
            dao.getReminderById(id)?.let { alarmScheduler.schedule(it) }
        }
        dao.setCompleted(id, completed)
    }

    suspend fun deleteAllCompleted() {
        dao.getCompletedRemindersSnapshot().forEach { alarmScheduler.cancel(it.id) }
        dao.deleteAllCompleted()
    }
}
