package com.remnd.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.remnd.data.AlarmScheduler
import com.remnd.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all alarms after the device reboots, since AlarmManager
 * does not persist across reboots.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                context.applicationContext,
                ReminderDatabase::class.java,
                ReminderDatabase.DATABASE_NAME
            )
                .addMigrations(ReminderDatabase.MIGRATION_1_2)
                .build()

            val reminders = db.reminderDao().getRemindersWithAlarms()
            val alarmScheduler = AlarmScheduler(context.applicationContext)
            reminders.forEach { alarmScheduler.schedule(it) }

            db.close()
        }
    }
}
