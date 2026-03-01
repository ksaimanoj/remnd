package com.remnd.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.remnd.data.AlarmScheduler
import com.remnd.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }

        if (intent.action == ACTION_COMPLETE) {
            val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
            if (reminderId == -1L) return

            // Cancel both main and early alarms
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            listOf(reminderId.toInt(), reminderId.toInt() + AlarmScheduler.EARLY_REQUEST_CODE_OFFSET)
                .forEach { rc ->
                    alarmManager.cancel(
                        PendingIntent.getBroadcast(
                            context, rc, Intent(context, AlarmReceiver::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }

            // Mark completed in DB
            CoroutineScope(Dispatchers.IO).launch {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    ReminderDatabase.DATABASE_NAME
                )
                    .addMigrations(ReminderDatabase.MIGRATION_1_2)
                    .build()
                db.reminderDao().setCompleted(reminderId, true)
                db.close()
            }
        }
        // ACTION_DISMISS: notification already cancelled above, nothing more to do
    }

    companion object {
        const val ACTION_COMPLETE = "com.remnd.action.COMPLETE"
        const val ACTION_DISMISS = "com.remnd.action.DISMISS"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
