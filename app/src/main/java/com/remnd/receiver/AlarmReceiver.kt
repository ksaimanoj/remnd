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

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, reminderId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            RemndApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description.ifBlank { null })
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), notification)

        // Reschedule the next occurrence for DAILY reminders
        if (frequencyType == FrequencyType.DAILY && dueTimeMillis != -1L) {
            val nextMillis = AlarmScheduler.nextOccurrenceMillis(dueTimeMillis)
            val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DESCRIPTION, description)
                putExtra(EXTRA_FREQUENCY_TYPE, frequencyType)
                putExtra(EXTRA_DUE_TIME_MILLIS, dueTimeMillis)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, reminderId.toInt(), nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, nextPendingIntent)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_FREQUENCY_TYPE = "frequency_type"
        const val EXTRA_DUE_TIME_MILLIS = "due_time_millis"
    }
}
