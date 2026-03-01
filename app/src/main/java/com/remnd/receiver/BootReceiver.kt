package com.remnd.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import com.remnd.data.ReminderDatabase
import androidx.room.Room
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
            ).build()

            val reminders = db.reminderDao().getRemindersWithAlarms()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            reminders.forEach { reminder ->
                val triggerMillis = reminder.dueTimeMillis ?: return@forEach
                if (triggerMillis <= System.currentTimeMillis()) return@forEach

                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
                    putExtra(AlarmReceiver.EXTRA_DESCRIPTION, reminder.description)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, reminder.id.toInt(), alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                )
            }

            db.close()
        }
    }
}
