package com.remnd.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.remnd.MainActivity
import com.remnd.R
import com.remnd.RemndApplication
import com.remnd.data.AlarmScheduler
import com.remnd.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        // Update or cancel the group summary based on remaining active notifications
        val remaining = notificationManager.activeNotifications.count {
            it.groupKey?.endsWith(RemndApplication.NOTIFICATION_GROUP_KEY) == true &&
                it.id != RemndApplication.SUMMARY_NOTIFICATION_ID
        }
        when {
            remaining == 0 -> notificationManager.cancel(RemndApplication.SUMMARY_NOTIFICATION_ID)
            else -> {
                val summaryTapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val summaryTapPi = PendingIntent.getActivity(
                    context, RemndApplication.SUMMARY_NOTIFICATION_ID, summaryTapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val summaryNotification = NotificationCompat.Builder(
                    context,
                    RemndApplication.NOTIFICATION_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("$remaining reminders")
                    .setContentIntent(summaryTapPi)
                    .setAutoCancel(true)
                    .setGroup(RemndApplication.NOTIFICATION_GROUP_KEY)
                    .setGroupSummary(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                notificationManager.notify(RemndApplication.SUMMARY_NOTIFICATION_ID, summaryNotification)
            }
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
