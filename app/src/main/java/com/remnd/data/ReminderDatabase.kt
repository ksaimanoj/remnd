package com.remnd.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Reminder::class],
    version = 1,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "remnd_db"
    }
}
