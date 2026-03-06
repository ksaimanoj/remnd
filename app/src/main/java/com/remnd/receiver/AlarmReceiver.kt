package com.remnd.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.remnd.MainActivity
import com.remnd.R
import com.remnd.RemndApplication
import com.remnd.data.AlarmScheduler
import com.remnd.data.FrequencyType

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val frequencyType = intent.getIntExtra(EXTRA_FREQUENCY_TYPE, FrequencyType.NONE)
        val dueTimeMillis = intent.getLongExtra(EXTRA_DUE_TIME_MILLIS, -1L)
        val isEarly = intent.getBooleanExtra(EXTRA_IS_EARLY, false)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, reminderId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = reminderId.toInt()

        // Build Complete action
        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COMPLETE
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val completePi = PendingIntent.getBroadcast(
            context, reminderId.toInt() + 2_000_000, completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build Dismiss action
        val dismissIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_DISMISS
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context, reminderId.toInt() + 3_000_000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifTitle = if (isEarly) "Reminder in 5 minutes" else title
        val notifText = if (isEarly) title else description.ifBlank { null }

        val notification = NotificationCompat.Builder(
            context,
            RemndApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notifTitle)
            .setContentText(notifText)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Complete \u2713", completePi)
            .addAction(0, "Dismiss", dismissPi)
            .setGroup(RemndApplication.NOTIFICATION_GROUP_KEY)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)

        // Count active reminder notifications (excluding the summary) to update the group summary
        val activeCount = notificationManager.activeNotifications.count {
            it.groupKey?.endsWith(RemndApplication.NOTIFICATION_GROUP_KEY) == true &&
                it.id != RemndApplication.SUMMARY_NOTIFICATION_ID
        }
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
            .setContentTitle("$activeCount reminders")
            .setContentIntent(summaryTapPi)
            .setAutoCancel(true)
            .setGroup(RemndApplication.NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(RemndApplication.SUMMARY_NOTIFICATION_ID, summaryNotification)

        // Reschedule the next occurrence for DAILY and HOURLY reminders (only on main alarm, not early)
        val isRecurring = !isEarly && (frequencyType == FrequencyType.DAILY || frequencyType == FrequencyType.HOURLY) && dueTimeMillis != -1L
        if (isRecurring) {
            val nextMillis = if (frequencyType == FrequencyType.HOURLY)
                AlarmScheduler.nextHourlyOccurrenceMillis(dueTimeMillis)
            else
                AlarmScheduler.nextOccurrenceMillis(dueTimeMillis)
            val now = System.currentTimeMillis()

            // Reschedule main alarm
            val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DESCRIPTION, description)
                putExtra(EXTRA_FREQUENCY_TYPE, frequencyType)
                putExtra(EXTRA_DUE_TIME_MILLIS, dueTimeMillis)
                putExtra(EXTRA_IS_EARLY, false)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, reminderId.toInt(), nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, nextPendingIntent)

            // Reschedule early alarm for next occurrence
            val earlyNextMillis = nextMillis - 5 * 60_000L
            if (earlyNextMillis > now) {
                val earlyNextIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(EXTRA_REMINDER_ID, reminderId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_DESCRIPTION, description)
                    putExtra(EXTRA_FREQUENCY_TYPE, frequencyType)
                    putExtra(EXTRA_DUE_TIME_MILLIS, dueTimeMillis)
                    putExtra(EXTRA_IS_EARLY, true)
                }
                val earlyNextPi = PendingIntent.getBroadcast(
                    context, reminderId.toInt() + AlarmScheduler.EARLY_REQUEST_CODE_OFFSET,
                    earlyNextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earlyNextMillis, earlyNextPi)
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_FREQUENCY_TYPE = "frequency_type"
        const val EXTRA_DUE_TIME_MILLIS = "due_time_millis"
        const val EXTRA_IS_EARLY = "is_early"
    }
}
